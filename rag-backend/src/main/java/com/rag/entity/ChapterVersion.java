package com.rag.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "chapter_versions", indexes = {
        @Index(name = "idx_chapter_versions_chapter_id", columnList = "chapter_id")
}, uniqueConstraints = {
        @UniqueConstraint(columnNames = {"chapter_id", "version_number"})
})
public class ChapterVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String outline;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "word_count", nullable = false)
    private Integer wordCount = 0;

    @Column(nullable = false, length = 20)
    private String status = "OUTLINE";

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(nullable = false, length = 30)
    private String source;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
