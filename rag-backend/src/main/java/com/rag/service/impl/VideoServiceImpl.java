package com.rag.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rag.dto.VideoTaskRequest;
import com.rag.dto.VideoTaskResponse;
import com.rag.entity.VideoTask;
import com.rag.repository.VideoTaskRepository;
import com.rag.security.SecurityUtils;
import com.rag.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {

    private final VideoTaskRepository videoTaskRepository;
    private final SecurityUtils securityUtils;
    private final ObjectMapper objectMapper;

    @Value("${kling.api.base-url:https://api.qnaigc.com/v1}")
    private String baseUrl;

    @Value("${kling.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    // ==================== 公开接口 ====================

    @Override
    @Transactional
    public VideoTaskResponse createTask(VideoTaskRequest request) {
        Long userId = securityUtils.getCurrentUserId();

        // 调用 Kling API 创建视频任务
        JsonNode apiResponse = callCreateVideo(request);

        String taskId = apiResponse.path("id").asText();
        String status = apiResponse.path("status").asText("queued");

        VideoTask task = new VideoTask();
        task.setUserId(userId);
        task.setTaskId(taskId);
        task.setModel("kling-v2-5-turbo");
        task.setPrompt(request.getPrompt());
        task.setStatus(status);
        task.setDuration(request.getDuration() != null ? request.getDuration() : 5);
        task.setSize(request.getSize() != null ? request.getSize() : "1280x720");
        task.setMode(request.getMode() != null ? request.getMode() : "std");
        videoTaskRepository.save(task);

        return toResponse(task);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VideoTaskResponse> listTasks() {
        Long userId = securityUtils.getCurrentUserId();
        return videoTaskRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public VideoTaskResponse getTask(Long id) {
        Long userId = securityUtils.getCurrentUserId();
        VideoTask task = videoTaskRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("视频任务不存在"));

        // 如果任务未完成，自动刷新一次状态
        if (isProcessing(task.getStatus())) {
            syncTaskStatus(task);
        }

        return toResponse(task);
    }

    @Override
    @Transactional
    public void deleteTask(Long id) {
        Long userId = securityUtils.getCurrentUserId();
        VideoTask task = videoTaskRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("视频任务不存在"));
        videoTaskRepository.delete(task);
    }

    @Override
    @Transactional
    public VideoTaskResponse refreshTask(Long id) {
        Long userId = securityUtils.getCurrentUserId();
        VideoTask task = videoTaskRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("视频任务不存在"));
        syncTaskStatus(task);
        return toResponse(task);
    }

    // ==================== 定时轮询 ====================

    /** 每 30 秒轮询一次未完成的任务状态 */
    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void pollPendingTasks() {
        List<VideoTask> pendingTasks = videoTaskRepository
                .findByStatusIn(List.of("queued", "initializing", "in_progress", "downloading", "uploading"));

        for (VideoTask task : pendingTasks) {
            try {
                syncTaskStatus(task);
            } catch (Exception e) {
                log.warn("轮询视频任务状态失败: taskId={}, error={}", task.getTaskId(), e.getMessage());
            }
        }
    }

    // ==================== 内部方法 ====================

    private JsonNode callCreateVideo(VideoTaskRequest request) {
        String url = baseUrl + "/videos";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = new java.util.HashMap<>();
        body.put("model", "kling-v2-5-turbo");
        body.put("prompt", request.getPrompt());
        body.put("seconds", String.valueOf(request.getDuration() != null ? request.getDuration() : 5));
        body.put("size", request.getSize() != null ? request.getSize() : "1280x720");
        body.put("mode", request.getMode() != null ? request.getMode() : "std");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            log.error("调用 Kling API 创建视频失败", e);
            throw new RuntimeException("调用视频生成 API 失败: " + e.getMessage());
        }
    }

    private JsonNode callGetVideoStatus(String taskId) {
        String url = baseUrl + "/videos/" + taskId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            log.error("查询 Kling 视频状态失败: taskId={}", taskId, e);
            throw new RuntimeException("查询视频状态失败: " + e.getMessage());
        }
    }

    private void syncTaskStatus(VideoTask task) {
        JsonNode result = callGetVideoStatus(task.getTaskId());

        String newStatus = result.path("status").asText(task.getStatus());
        task.setStatus(newStatus);

        // 完成时提取视频 URL：Kling 格式为 task_result.videos[0].url
        if ("completed".equals(newStatus)) {
            JsonNode videos = result.path("task_result").path("videos");
            if (videos.isArray() && videos.size() > 0) {
                JsonNode first = videos.get(0);
                task.setVideoUrl(first.path("url").asText(null));
            }
        }

        // 失败时提取错误信息
        if ("failed".equals(newStatus)) {
            task.setErrorMessage(result.path("error").path("message").asText(
                    result.path("error_message").asText("视频生成失败")));
        }

        videoTaskRepository.save(task);
        log.info("视频任务状态更新: taskId={}, status={}, videoUrl={}", task.getTaskId(), newStatus, task.getVideoUrl());
    }

    private boolean isProcessing(String status) {
        return status != null && !status.equals("completed") && !status.equals("failed");
    }

    private VideoTaskResponse toResponse(VideoTask task) {
        VideoTaskResponse resp = new VideoTaskResponse();
        resp.setId(task.getId());
        resp.setTaskId(task.getTaskId());
        resp.setModel(task.getModel());
        resp.setPrompt(task.getPrompt());
        resp.setStatus(task.getStatus());
        resp.setDuration(task.getDuration());
        resp.setSize(task.getSize());
        resp.setMode(task.getMode());
        resp.setVideoUrl(task.getVideoUrl());
        resp.setCoverUrl(task.getCoverUrl());
        resp.setErrorMessage(task.getErrorMessage());
        resp.setCreatedAt(task.getCreatedAt());
        resp.setUpdatedAt(task.getUpdatedAt());
        return resp;
    }
}
