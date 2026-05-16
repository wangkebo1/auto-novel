package com.rag.dto;

import lombok.Data;

@Data
public class RewriteRequest {
    private String content;
    private String style; // vivid(生动), concise(简洁), professional(专业), ancient(古风), humorous(幽默)
}
