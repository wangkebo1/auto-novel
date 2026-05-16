package com.rag.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rag.dto.ExtractedCharacter;
import com.rag.entity.Chapter;
import com.rag.entity.Novel;
import com.rag.repository.ChapterRepository;
import com.rag.repository.NovelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CharacterExtractService {

    private final ChatClient chatClient;
    private final NovelRepository novelRepository;
    private final ChapterRepository chapterRepository;
    private final ObjectMapper objectMapper;

    public List<ExtractedCharacter> extractCharacters(Long novelId) {
        Novel novel = novelRepository.findById(novelId)
                .orElseThrow(() -> new RuntimeException("小说不存在"));

        List<Chapter> chapters = chapterRepository.findByNovelIdOrderByChapterNumberAsc(novelId);
        StringBuilder content = new StringBuilder();
        for (Chapter ch : chapters) {
            if (ch.getContent() != null && !ch.getContent().isEmpty()) {
                content.append(ch.getContent()).append("\n\n");
            }
        }

        if (content.isEmpty()) {
            return List.of();
        }

        String prompt = String.format("""
                请分析以下小说内容，提取出所有出现的角色。

                小说内容：
                %s

                要求：
                1. 提取所有重要角色（主角、反派、配角）
                2. 分析角色性格特点
                3. 总结角色背景
                4. 返回 JSON 数组格式：[{"name":"角色名","roleType":"PROTAGONIST/ANTAGONIST/SUPPORTING","personality":"性格描述","background":"背景描述"}]
                5. 只返回 JSON，不要其他文字
                """, content.substring(0, Math.min(content.length(), 8000)));

        try {
            String result = chatClient.prompt().user(prompt).call().content();
            int start = result.indexOf('[');
            int end = result.lastIndexOf(']');
            if (start >= 0 && end > start) {
                String json = result.substring(start, end + 1);
                List<ExtractedCharacter> characters = objectMapper.readValue(json, new TypeReference<>() {});

                // 去重：按角色名去重
                java.util.Map<String, ExtractedCharacter> uniqueChars = new java.util.LinkedHashMap<>();
                for (ExtractedCharacter ch : characters) {
                    uniqueChars.putIfAbsent(ch.getName(), ch);
                }

                return new java.util.ArrayList<>(uniqueChars.values());
            }
        } catch (Exception e) {
            log.error("解析角色失败", e);
        }
        return List.of();
    }
}
