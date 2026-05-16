package com.rag.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "novels", indexes = {
        @Index(name = "idx_novels_user_id", columnList = "user_id")
})
public class Novel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 50)
    private String genre;

    @Column(length = 50)
    private String style;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "world_setting", columnDefinition = "TEXT")
    private String worldSetting;

    @Column(nullable = false, length = 20)
    private String status = "DRAFT";

    @Column(name = "total_words", nullable = false)
    private Integer totalWords = 0;

    @Column(name = "cover_url", columnDefinition = "TEXT")
    private String coverUrl;

    @OneToMany(mappedBy = "novel", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("chapterNumber ASC")
    private List<Chapter> chapters = new ArrayList<>();

    @OneToMany(mappedBy = "novel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NovelCharacter> characters = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
