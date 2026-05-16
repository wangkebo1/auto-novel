package com.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    /** LLM 生成的回答 */
    private String answer;

    /** 检索到的参考来源文件名列表 */
    private List<String> sources;

    /** 检索到的原始文本片段（调试用，可前端选择是否展示） */
    private List<SourceChunk> chunks;

    private LocalDateTime timestamp;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SourceChunk {
        private String content;
        private String fileName;
        private Double score;
    }
}
