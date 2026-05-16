package com.rag.common.dto;

import lombok.Data;

/**
 * 注册请求
 */
@Data
public class RegisterRequest {
    private String username;
    private String email;
    private String password;
    private String nickname;
}
