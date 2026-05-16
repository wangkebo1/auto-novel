package com.rag.controller;

import com.rag.dto.Result;
import com.rag.dto.VideoTaskRequest;
import com.rag.dto.VideoTaskResponse;
import com.rag.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    /** 创建视频生成任务 */
    @PostMapping
    public Result<VideoTaskResponse> createTask(@RequestBody VideoTaskRequest request) {
        log.info("创建视频任务: prompt={}", request.getPrompt());
        return Result.ok(videoService.createTask(request));
    }

    /** 查询当前用户的所有视频任务 */
    @GetMapping
    public Result<List<VideoTaskResponse>> listTasks() {
        return Result.ok(videoService.listTasks());
    }

    /** 查询单个任务详情 */
    @GetMapping("/{id}")
    public Result<VideoTaskResponse> getTask(@PathVariable Long id) {
        return Result.ok(videoService.getTask(id));
    }

    /** 手动刷新任务状态 */
    @PostMapping("/{id}/refresh")
    public Result<VideoTaskResponse> refreshTask(@PathVariable Long id) {
        return Result.ok(videoService.refreshTask(id));
    }

    /** 删除任务 */
    @DeleteMapping("/{id}")
    public Result<Void> deleteTask(@PathVariable Long id) {
        videoService.deleteTask(id);
        return Result.ok();
    }
}
