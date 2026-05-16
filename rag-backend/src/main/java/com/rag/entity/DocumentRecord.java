package com.rag.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "document_record", indexes = {
        @Index(name = "idx_doc_hash", columnList = "file_hash", unique = true),
        @Index(name = "idx_doc_kb", columnList = "knowledge_base_id")
})
public class DocumentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 原始文件名 */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    /** 服务器存储路径 */
    @Column(name = "file_path", length = 1000)
    private String filePath;

    /** SHA-256 文件哈希，用于去重 */
    @Column(name = "file_hash", nullable = false, length = 64, unique = true)
    private String fileHash;

    /** 文件大小（字节） */
    @Column(name = "file_size")
    private Long fileSize;

    /** 文件类型 (pdf/docx/xlsx...) */
    @Column(name = "file_type", length = 50)
    private String fileType;

    /** 分块数量 */
    @Column(name = "chunk_count")
    private Integer chunkCount = 0;

    /** 处理状态: PROCESSING / PROCESSED / FAILED */
    @Column(length = 50)
    private String status = "PROCESSING";

    /** 错误信息 */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @JsonIgnoreProperties({"documents"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "knowledge_base_id")
    private KnowledgeBase knowledgeBase;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
