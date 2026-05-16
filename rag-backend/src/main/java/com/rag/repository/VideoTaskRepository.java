package com.rag.repository;

import com.rag.entity.VideoTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VideoTaskRepository extends JpaRepository<VideoTask, Long> {
    List<VideoTask> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<VideoTask> findByTaskId(String taskId);
    Optional<VideoTask> findByIdAndUserId(Long id, Long userId);
    List<VideoTask> findByStatusIn(List<String> statuses);
}
