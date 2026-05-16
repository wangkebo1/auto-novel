package com.rag.dto;

import lombok.Data;

@Data
public class NovelRequest {
    private String title;
    private String genre;
    private String style;
    private String description;
    private String worldSetting;
    private String coverUrl;
}
