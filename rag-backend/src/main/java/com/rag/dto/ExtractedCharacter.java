package com.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ExtractedCharacter {
    private String name;
    private String roleType;
    private String personality;
    private String background;
}
