package com.rag.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class NovelResponse {
    private Long id;
    private String title;
    private String genre;
    private String style;
    private String description;
    private String worldSetting;
    private String status;
    private Integer totalWords;
    private Integer chapterCount;
    private String coverUrl;
    private List<ChapterBrief> chapters;
    private List<CharacterInfo> characters;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class ChapterBrief {
        private Long id;
        private Integer chapterNumber;
        private String title;
        private String outline;
        private Integer wordCount;
        private String status;
        private LocalDateTime completedAt;
    }

    @Data
    @Builder
    public static class CharacterInfo {
        private Long id;
        private String name;
        private String roleType;
        private String personality;
        private String background;
        private String appearance;
        private String relationships;
    }
}
