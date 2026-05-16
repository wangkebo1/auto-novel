package com.rag.dto;

import lombok.Data;

@Data
public class CharacterRequest {
    private String name;
    private String roleType;
    private String personality;
    private String background;
    private String appearance;
    private String relationships;
}
