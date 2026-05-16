package com.rag.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录响应（包含 Token）
 */
@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String username;
    private String email;
    private String nickname;
}
