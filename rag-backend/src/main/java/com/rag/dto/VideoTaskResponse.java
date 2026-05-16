package com.rag.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VideoTaskResponse {
    private Long id;
    private String taskId;
    private String model;
    private String prompt;
    private String status;
    private Integer duration;
    private String size;
    private String mode;
    private String videoUrl;
    private String coverUrl;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
