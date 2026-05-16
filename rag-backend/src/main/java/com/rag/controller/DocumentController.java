package com.rag.controller;

import com.rag.dto.Result;
import com.rag.dto.UploadResponse;
import com.rag.entity.DocumentRecord;
import com.rag.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    /**
     * POST /api/documents/upload
     * 上传文档到指定知识库，执行解析 + 分块 + 向量化
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<UploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("knowledgeBaseId") Long knowledgeBaseId) {

        if (file.isEmpty()) {
            return Result.fail("文件不能为空");
        }

        log.info("接收到文件上传请求: {}, 大小: {} bytes, 知识库: {}",
                file.getOriginalFilename(), file.getSize(), knowledgeBaseId);

        UploadResponse response = documentService.uploadDocument(file, knowledgeBaseId);
        return Result.ok(response);
    }

    /**
     * GET /api/documents?knowledgeBaseId=1
     * 查询知识库下的所有文档列表
     */
    @GetMapping
    public Result<List<DocumentRecord>> list(@RequestParam Long knowledgeBaseId) {
        List<DocumentRecord> documents = documentService.listDocuments(knowledgeBaseId);
        return Result.ok(documents);
    }

    /**
     * DELETE /api/documents/{id}
     * 删除文档及其向量数据
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        documentService.deleteDocument(id);
        return Result.ok();
    }
}
