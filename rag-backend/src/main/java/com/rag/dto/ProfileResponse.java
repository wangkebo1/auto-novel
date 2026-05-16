package com.rag.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProfileResponse {
    private Long id;
    private String username;
    private String email;
    private String nickname;
    private Integer points;
    private String roles;
}
