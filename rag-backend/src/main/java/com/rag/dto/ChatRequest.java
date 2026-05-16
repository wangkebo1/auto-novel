package com.rag.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatRequest {

    @Size(max = 2000, message = "闂闀垮害涓嶈兘瓒呰繃 2000 瀛楃")
    private String message;

    /** 鎸囧畾鐭ヨ瘑搴?ID锛屼负 null 鍒欐悳绱㈡墍鏈夌煡璇嗗簱 */
    private Long knowledgeBaseId;

    /** 闂鍙互鍙鍖栨墍鏈夌瓑鍥戒簲锛? */
    private ChatImage image;

    /** 杩斿洖鐨勫弬鑰冩潵婧愭暟閲?*/
    private Integer topK = 5;

    @Data
    public static class ChatImage {
        private String fileName;
        private String mimeType;
        private String base64;
    }
}
