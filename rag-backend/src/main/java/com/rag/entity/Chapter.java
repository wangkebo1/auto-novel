package com.rag.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "chapters", indexes = {
        @Index(name = "idx_chapters_novel_id", columnList = "novel_id")
}, uniqueConstraints = {
        @UniqueConstraint(columnNames = {"novel_id", "chapter_number"})
})
public class Chapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "novel_id", nullable = false)
    private Novel novel;

    @Column(name = "chapter_number", nullable = false)
    private Integer chapterNumber;

    @Column(length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String outline;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "word_count", nullable = false)
    private Integer wordCount = 0;

    @Column(nullable = false, length = 20)
    private String status = "OUTLINE";

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
