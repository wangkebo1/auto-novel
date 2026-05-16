package com.rag.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "knowledge_base")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class KnowledgeBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** 是否启用 */
    @Column(nullable = false)
    private Boolean enabled = true;

    /** 所属用户ID（权限隔离） */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"roles", "password"})
    private User user;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @JsonIgnoreProperties({"knowledgeBase"})
    @OneToMany(mappedBy = "knowledgeBase", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DocumentRecord> documents = new ArrayList<>();

    public KnowledgeBase(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
