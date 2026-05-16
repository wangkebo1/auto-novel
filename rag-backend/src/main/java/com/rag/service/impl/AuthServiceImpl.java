package com.rag.service.impl;

import com.rag.dto.AuthResponse;
import com.rag.dto.LoginRequest;
import com.rag.dto.RegisterRequest;
import com.rag.entity.Role;
import com.rag.entity.User;
import com.rag.repository.RoleRepository;
import com.rag.repository.UserRepository;
import com.rag.security.JwtTokenUtil;
import com.rag.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        // 检查邮箱是否已存在（仅当提供了邮箱时）
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("邮箱已被注册");
        }

        // 创建新用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail() != null && !request.getEmail().isBlank() ? request.getEmail() : null);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setEnabled(true);

        // 分配默认角色 ROLE_USER
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("默认角色不存在"));
        user.setRoles(Set.of(userRole));

        userRepository.save(user);

        // 自动登录，生成 Token
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities("ROLE_USER")
                .build();
        String token = jwtTokenUtil.generateToken(userDetails);

        String roles = user.getRoles().stream().map(r -> r.getName()).collect(java.util.stream.Collectors.joining(","));
        return new AuthResponse(token, user.getUsername(), user.getEmail(), user.getNickname(), roles);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        // Spring Security 认证
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        // 认证成功，生成 Token
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtTokenUtil.generateToken(userDetails);

        // 获取用户信息
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        String roles = user.getRoles().stream().map(r -> r.getName()).collect(java.util.stream.Collectors.joining(","));
        return new AuthResponse(token, user.getUsername(), user.getEmail(), user.getNickname(), roles);
    }
}
