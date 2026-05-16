package com.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadResponse {

    private Long documentId;
    private String fileName;
    private Integer chunkCount;
    private String status;
    private String message;

    public static UploadResponse success(Long docId, String fileName, int chunkCount) {
        return UploadResponse.builder()
                .documentId(docId)
                .fileName(fileName)
                .chunkCount(chunkCount)
                .status("PROCESSED")
                .message("文档上传并向量化成功，共生成 " + chunkCount + " 个文本块")
                .build();
    }

    public static UploadResponse duplicate(String fileName) {
        return UploadResponse.builder()
                .fileName(fileName)
                .status("DUPLICATE")
                .message("文档已存在，跳过重复上传")
                .build();
    }

    public static UploadResponse failed(String fileName, String errorMessage) {
        return UploadResponse.builder()
                .fileName(fileName)
                .status("FAILED")
                .message("文档处理失败: " + errorMessage)
                .build();
    }
}
