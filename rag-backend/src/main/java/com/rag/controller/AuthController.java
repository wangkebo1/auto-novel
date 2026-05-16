package com.rag.controller;

import com.rag.dto.AuthResponse;
import com.rag.dto.LoginRequest;
import com.rag.dto.RegisterRequest;
import com.rag.dto.Result;
import com.rag.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器 - 登录、注册
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register
     * 用户注册
     */
    @PostMapping("/register")
    public Result<AuthResponse> register(@RequestBody RegisterRequest request) {
        log.info("用户注册: {}", request.getUsername());
        try {
            AuthResponse response = authService.register(request);
            return Result.ok(response);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * POST /api/auth/login
     * 用户登录
     */
    @PostMapping("/login")
    public Result<AuthResponse> login(@RequestBody LoginRequest request) {
        log.info("用户登录: {}", request.getUsername());
        try {
            AuthResponse response = authService.login(request);
            return Result.ok(response);
        } catch (RuntimeException e) {
            return Result.fail("用户名或密码错误");
        }
    }
}
