package com.rag.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatRequest {

    @NotBlank(message = "问题不能为空")
    @Size(max = 2000, message = "问题长度不能超过 2000 字符")
    private String message;

    /** 指定知识库 ID，为 null 则搜索所有知识库 */
    private Long knowledgeBaseId;

    /** 返回的参考来源数量 */
    private Integer topK = 5;
}
