package com.rag.service;

import com.rag.dto.UploadResponse;
import com.rag.entity.DocumentRecord;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {

    /**
     * 上传并向量化文档
     *
     * @param file            上传的文件 (PDF/Word/Excel...)
     * @param knowledgeBaseId 目标知识库 ID
     * @return 处理结果
     */
    UploadResponse uploadDocument(MultipartFile file, Long knowledgeBaseId);

    /**
     * 查询某知识库下所有文档
     */
    List<DocumentRecord> listDocuments(Long knowledgeBaseId);

    /**
     * 删除文档（同时从向量数据库移除对应向量）
     */
    void deleteDocument(Long documentId);
}
