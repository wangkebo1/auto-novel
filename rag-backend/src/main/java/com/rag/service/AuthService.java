package com.rag.service;

import com.rag.dto.AuthResponse;
import com.rag.dto.LoginRequest;
import com.rag.dto.RegisterRequest;

public interface AuthService {
    
    /**
     * 用户注册
     */
    AuthResponse register(RegisterRequest request);

    /**
     * 用户登录
     */
    AuthResponse login(LoginRequest request);
}
