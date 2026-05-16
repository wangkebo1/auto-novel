package com.rag.security;

import com.rag.entity.User;
import com.rag.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * 获取当前登录用户信息的工具类
 */
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    /**
     * 获取当前登录用户的用户名
     */
    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }
        return null;
    }

    /**
     * 获取当前登录用户实体
     */
    public User getCurrentUser() {
        String username = getCurrentUsername();
        if (username == null) {
            throw new RuntimeException("未登录");
        }
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    /**
     * 获取当前登录用户ID
     */
    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }
}
