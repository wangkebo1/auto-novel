package com.rag.service.impl;

import com.rag.dto.UploadResponse;
import com.rag.entity.DocumentRecord;
import com.rag.entity.KnowledgeBase;
import com.rag.repository.DocumentRecordRepository;
import com.rag.repository.KnowledgeBaseRepository;
import com.rag.parser.DocumentParser;
import com.rag.parser.DocumentParserFactory;
import com.rag.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final VectorStore vectorStore;
    private final DocumentRecordRepository documentRecordRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final DocumentParserFactory parserFactory;

    @Value("${rag.upload.path:./uploads}")
    private String uploadPath;

    @Value("${rag.chunk.size:512}")
    private int chunkSize;

    @Value("${rag.chunk.min-size:128}")
    private int minChunkSize;

    @Override
    @Transactional
    public UploadResponse uploadDocument(MultipartFile file, Long knowledgeBaseId) {
        String originalFileName = file.getOriginalFilename();
        log.info("开始处理文档: {}, 知识库ID: {}", originalFileName, knowledgeBaseId);

        // 1. 计算文件 SHA-256 哈希，用于去重
        String fileHash;
        try {
            fileHash = computeFileHash(file.getBytes());
        } catch (IOException e) {
            log.error("读取文件内容失败", e);
            return UploadResponse.failed(originalFileName, "文件读取失败");
        }

        // 2. 检查文档是否已成功处理（只有 PROCESSED 状态才跳过，FAILED 允许重试）
        if (documentRecordRepository.existsByFileHashAndStatus(fileHash, "PROCESSED")) {
            log.info("文档 [{}] 已成功处理 (hash={}), 跳过重复处理", originalFileName, fileHash);
            return UploadResponse.duplicate(originalFileName);
        }
        // 清理同hash的旧FAILED记录
        documentRecordRepository.findByFileHash(fileHash).ifPresent(old -> {
            if ("FAILED".equals(old.getStatus()) || "PROCESSING".equals(old.getStatus())) {
                documentRecordRepository.delete(old);
            }
        });

        // 3. 查找知识库
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(knowledgeBaseId)
                .orElseThrow(() -> new RuntimeException("知识库不存在: " + knowledgeBaseId));

        // 4. 预先保存文档记录（状态 PROCESSING）
        DocumentRecord record = new DocumentRecord();
        record.setFileName(originalFileName);
        record.setFileHash(fileHash);
        record.setFileSize(file.getSize());
        record.setFileType(extractFileType(originalFileName));
        record.setStatus("PROCESSING");
        record.setKnowledgeBase(knowledgeBase);
        record = documentRecordRepository.save(record);

        try {
            // 5. 保存文件到磁盘
            String storedFilePath = saveFileToDisk(file, fileHash);
            record.setFilePath(storedFilePath);

            // 6. 根据文件格式选择专用解析器（Word/Excel 结构化解析，其余 Tika 兜底）
            String fileExt = extractFileType(originalFileName);
            DocumentParser parser = parserFactory.getParser(fileExt);
            List<Document> rawDocuments = parser.parse(java.nio.file.Paths.get(storedFilePath));
            log.info("文档 [{}] (格式:{}) 解析完成，原始段落数: {}", originalFileName, fileExt, rawDocuments.size());

            // 7. 文本分块（按 Token 数切割，保留句子完整性）
            TokenTextSplitter splitter = new TokenTextSplitter(chunkSize, minChunkSize, 5, 10000, true);
            List<Document> chunks = splitter.apply(rawDocuments);
            log.info("文档 [{}] 分块完成，共 {} 个文本块", originalFileName, chunks.size());

            // 8. 为每个 chunk 注入元数据（用于后续过滤检索）
            final Long kbId = knowledgeBaseId;
            final String docName = originalFileName;
            final Long docId = record.getId();
            chunks.forEach(chunk -> {
                Map<String, Object> metadata = chunk.getMetadata();
                metadata.put("knowledgeBaseId", kbId.toString());
                metadata.put("fileName", docName);
                metadata.put("documentId", docId.toString());
                metadata.put("fileHash", fileHash);
            });

            // 9. 向量化并存入 PGVector
            vectorStore.add(chunks);
            log.info("文档 [{}] 向量化完成，存入 {} 个向量", originalFileName, chunks.size());

            // 10. 更新文档记录状态
            record.setChunkCount(chunks.size());
            record.setStatus("PROCESSED");
            documentRecordRepository.save(record);

            return UploadResponse.success(record.getId(), originalFileName, chunks.size());

        } catch (Exception e) {
            log.error("文档处理失败: {}", originalFileName, e);
            record.setStatus("FAILED");
            record.setErrorMessage(e.getMessage());
            documentRecordRepository.save(record);
            return UploadResponse.failed(originalFileName, e.getMessage());
        }
    }

    @Override
    public List<DocumentRecord> listDocuments(Long knowledgeBaseId) {
        return documentRecordRepository.findByKnowledgeBaseId(knowledgeBaseId);
    }

    @Override
    @Transactional
    public void deleteDocument(Long documentId) {
        DocumentRecord record = documentRecordRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("文档不存在: " + documentId));

        // 从磁盘删除文件
        if (record.getFilePath() != null) {
            try {
                Files.deleteIfExists(Paths.get(record.getFilePath()));
            } catch (IOException e) {
                log.warn("删除文件失败: {}", record.getFilePath());
            }
        }

        // 注意：PGVector 暂不支持按 metadata filter 批量删除，
        // 需要存储向量 ID 才能精确删除。此处只删除数据库记录，
        // 向量数据在下次搜索时会因 documentId 过滤而不被使用。
        // Phase 3 完善：记录 vector IDs 实现精确删除。
        documentRecordRepository.deleteById(documentId);
        log.info("文档记录已删除: id={}, file={}", documentId, record.getFileName());
    }

    // ——————————————————————— 私有工具方法 ———————————————————————

    /**
     * 计算文件内容的 SHA-256 哈希
     */
    private String computeFileHash(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("文件哈希计算失败", e);
        }
    }

    /**
     * 将上传文件保存到磁盘，用 UUID 命名防止冲突
     */
    private String saveFileToDisk(MultipartFile file, String fileHash) throws IOException {
        String originalFileName = file.getOriginalFilename();
        String ext = extractFileType(originalFileName);
        String storedName = fileHash.substring(0, 16) + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8) + "." + ext;

        Path uploadDir = Paths.get(uploadPath);
        Files.createDirectories(uploadDir);

        Path targetPath = uploadDir.resolve(storedName);
        file.transferTo(targetPath.toFile());

        log.debug("文件已保存到: {}", targetPath.toAbsolutePath());
        return targetPath.toAbsolutePath().toString();
    }

    /**
     * 从文件名提取扩展名
     */
    private String extractFileType(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "bin";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
}
