package com.rag.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChapterVersionDetailResponse {
    private Long id;
    private Long chapterId;
    private Integer versionNumber;
    private String title;
    private String outline;
    private String content;
    private String summary;
    private String notes;
    private Integer wordCount;
    private String status;
    private String source;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
