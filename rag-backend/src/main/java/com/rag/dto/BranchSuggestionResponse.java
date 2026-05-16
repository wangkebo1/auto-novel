package com.rag.dto;

import lombok.Data;

@Data
public class BranchSuggestionResponse {
    private String title;
    private String direction;
    private String conflict;
    private String hook;
}
