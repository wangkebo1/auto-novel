package com.rag.dto;

import lombok.Data;

@Data
public class GenerateRequest {
    /** 生成内容的目标字数（默认 2000） */
    private Integer targetWords;
    /** 用户附加指令（如"本章要有打斗场面"） */
    private String userPrompt;
}
