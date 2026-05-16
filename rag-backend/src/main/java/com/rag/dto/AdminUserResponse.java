package com.rag.dto;

import lombok.Data;

@Data
public class AdminUserResponse {
    private Long id;
    private String username;
    private String email;
    private String nickname;
    private Boolean enabled;
    private String roles;
    private Integer novelCount;
    private Integer totalWords;
    private String createdAt;
}
