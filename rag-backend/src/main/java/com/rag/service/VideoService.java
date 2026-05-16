package com.rag.service;

import com.rag.dto.VideoTaskRequest;
import com.rag.dto.VideoTaskResponse;

import java.util.List;

public interface VideoService {
    /** 创建视频生成任务 */
    VideoTaskResponse createTask(VideoTaskRequest request);

    /** 查询当前用户的所有视频任务 */
    List<VideoTaskResponse> listTasks();

    /** 查询单个任务详情（并同步远端状态） */
    VideoTaskResponse getTask(Long id);

    /** 删除任务 */
    void deleteTask(Long id);

    /** 手动刷新任务状态（查询 Kling API） */
    VideoTaskResponse refreshTask(Long id);
}
