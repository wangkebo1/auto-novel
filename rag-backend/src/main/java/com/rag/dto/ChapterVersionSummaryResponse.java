package com.rag.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChapterVersionSummaryResponse {
    private Long id;
    private Long chapterId;
    private Integer versionNumber;
    private String title;
    private Integer wordCount;
    private String source;
    private String summaryPreview;
    private LocalDateTime createdAt;
}
