package com.rag.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChapterResponse {
    private Long id;
    private Long novelId;
    private Integer chapterNumber;
    private String title;
    private String outline;
    private String content;
    private String summary;
    private Integer wordCount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    private String notes;
}
