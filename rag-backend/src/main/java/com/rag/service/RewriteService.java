package com.rag.service;

import com.rag.dto.RewriteRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RewriteService {

    private final ChatClient chatClient;

    public String rewrite(RewriteRequest request) {
        String stylePrompt = getStylePrompt(request.getStyle());
        String prompt = String.format("""
                请将以下文本改写为%s风格：

                %s

                要求：
                1. 保持原意不变
                2. 字数相近
                3. 直接输出改写后的内容，不要任何解释
                """, stylePrompt, request.getContent());

        return chatClient.prompt().user(prompt).call().content();
    }

    private String getStylePrompt(String style) {
        return switch (style) {
            case "vivid" -> "生动形象、富有画面感";
            case "concise" -> "简洁明了、言简意赅";
            case "professional" -> "专业严谨、书面化";
            case "ancient" -> "古风雅致、文言文";
            case "humorous" -> "幽默风趣、轻松活泼";
            default -> "更优美流畅";
        };
    }
}
