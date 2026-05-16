package com.rag.dto;

import lombok.Data;

@Data
public class VideoTaskRequest {
    private String prompt;
    private Integer duration = 5;      // 5 或 10 秒
    private String size = "1280x720";  // 视频尺寸
    private String mode = "std";       // std / pro
}
