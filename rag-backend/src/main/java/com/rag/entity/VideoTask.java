package com.rag.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "video_tasks", indexes = {
        @Index(name = "idx_video_tasks_user_id", columnList = "user_id"),
        @Index(name = "idx_video_tasks_status", columnList = "status")
})
public class VideoTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "task_id", nullable = false, unique = true, length = 200)
    private String taskId;

    @Column(nullable = false, length = 50)
    private String model = "kling-v2-5-turbo";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String prompt;

    @Column(nullable = false, length = 30)
    private String status = "queued";

    @Column(nullable = false)
    private Integer duration = 5;

    @Column(nullable = false, length = 20)
    private String size = "1280x720";

    @Column(nullable = false, length = 10)
    private String mode = "std";

    @Column(name = "video_url", columnDefinition = "TEXT")
    private String videoUrl;

    @Column(name = "cover_url", columnDefinition = "TEXT")
    private String coverUrl;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
