package com.rag.controller;

import com.rag.dto.*;
import com.rag.entity.SensitiveWord;
import com.rag.entity.User;
import com.rag.repository.*;
import com.rag.service.BillingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final NovelRepository novelRepository;
    private final ChapterRepository chapterRepository;
    private final SensitiveWordRepository sensitiveWordRepository;
    private final BillingService billingService;

    // ==================== 统计 ====================

    @GetMapping("/statistics")
    public Result<AdminStatistics> getStatistics() {
        AdminStatistics stats = new AdminStatistics();
        stats.setTotalUsers(userRepository.count());
        stats.setTotalNovels(novelRepository.count());
        stats.setTotalChapters(chapterRepository.count());

        Long totalWords = novelRepository.findAll().stream()
                .mapToLong(n -> n.getTotalWords() != null ? n.getTotalWords() : 0)
                .sum();
        stats.setTotalWords(totalWords);

        return Result.ok(stats);
    }

    // ==================== 用户管理 ====================

    @GetMapping("/users")
    public Result<List<AdminUserResponse>> listUsers() {
        List<User> users = userRepository.findAll();
        List<AdminUserResponse> responses = users.stream().map(user -> {
            AdminUserResponse resp = new AdminUserResponse();
            resp.setId(user.getId());
            resp.setUsername(user.getUsername());
            resp.setEmail(user.getEmail());
            resp.setNickname(user.getNickname());
            resp.setEnabled(user.getEnabled());
            resp.setRoles(user.getRoles().stream().map(r -> r.getName()).collect(Collectors.joining(",")));
            resp.setCreatedAt(user.getCreatedAt().toString());

            int novelCount = novelRepository.countByUserId(user.getId());
            resp.setNovelCount(novelCount);

            int totalWords = novelRepository.findByUserId(user.getId()).stream()
                    .mapToInt(n -> n.getTotalWords() != null ? n.getTotalWords() : 0)
                    .sum();
            resp.setTotalWords(totalWords);

            return resp;
        }).collect(Collectors.toList());

        return Result.ok(responses);
    }

    @PutMapping("/users/{userId}/toggle")
    public Result<Void> toggleUserStatus(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setEnabled(!user.getEnabled());
        userRepository.save(user);
        return Result.ok(null);
    }

    // ==================== 敏感词管理 ====================

    @GetMapping("/sensitive-words")
    public Result<List<SensitiveWord>> listSensitiveWords() {
        return Result.ok(sensitiveWordRepository.findAll());
    }

    @PostMapping("/sensitive-words")
    public Result<SensitiveWord> addSensitiveWord(@RequestBody SensitiveWord word) {
        return Result.ok(sensitiveWordRepository.save(word));
    }

    @DeleteMapping("/sensitive-words/{id}")
    public Result<Void> deleteSensitiveWord(@PathVariable Long id) {
        sensitiveWordRepository.deleteById(id);
        return Result.ok(null);
    }

    // ==================== ???? ====================

    @GetMapping("/refunds")
    public Result<List<RefundOrderResponse>> listRefunds() {
        return Result.ok(billingService.listAllRefunds());
    }

    @PostMapping("/refunds/{refundId}/review")
    public Result<RefundOrderResponse> reviewRefund(
            @PathVariable Long refundId,
            @RequestBody RefundReviewRequest request) {
        return Result.ok(billingService.reviewRefund(refundId, request));
    }

    // ==================== 小说管理 ====================

    @GetMapping("/novels")
    public Result<List<NovelResponse>> listAllNovels() {
        return Result.ok(novelRepository.findAll().stream()
                .map(this::toNovelResponse)
                .collect(Collectors.toList()));
    }

    @DeleteMapping("/novels/{novelId}")
    public Result<Void> deleteNovel(@PathVariable Long novelId) {
        novelRepository.deleteById(novelId);
        return Result.ok(null);
    }

    private NovelResponse toNovelResponse(com.rag.entity.Novel novel) {
        return NovelResponse.builder()
                .id(novel.getId())
                .title(novel.getTitle())
                .genre(novel.getGenre())
                .status(novel.getStatus())
                .totalWords(novel.getTotalWords())
                .chapterCount(novel.getChapters() != null ? novel.getChapters().size() : 0)
                .createdAt(novel.getCreatedAt())
                .updatedAt(novel.getUpdatedAt())
                .build();
    }
}
