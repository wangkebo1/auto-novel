package com.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WordFrequency {
    private String word;
    private Integer count;
}
