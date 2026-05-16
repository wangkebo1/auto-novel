package com.rag.dto;

import lombok.Data;

@Data
public class GenerationStatus {
    /** IDLE / RUNNING / COMPLETED / STOPPED / ERROR */
    private String status;
    /** 当前正在生成第几章 */
    private Integer currentChapter;
    /** 总章节数 */
    private Integer totalChapters;
    /** 已完成章节数 */
    private Integer completedChapters;
    /** 当前章节标题 */
    private String currentChapterTitle;
    /** 消息 */
    private String message;
}
