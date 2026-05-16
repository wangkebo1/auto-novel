package com.rag.dto;

import lombok.Data;
import java.util.Map;

@Data
public class NovelStatistics {
    private Integer totalWords;
    private Integer totalChapters;
    private Integer completedChapters;
    private Integer averageWordsPerChapter;
    private Integer longestChapterWords;
    private Integer shortestChapterWords;
    private Map<String, Integer> characterAppearances;
    private Double completionRate;
}
