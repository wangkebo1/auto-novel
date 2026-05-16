package com.rag.controller;

import com.rag.dto.ChatRequest;
import com.rag.dto.ChatResponse;
import com.rag.dto.Result;
import com.rag.service.RagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final RagService ragService;

    /**
     * POST /api/chat
     * 同步 RAG 问答，返回完整回答
     */
    @PostMapping
    public Result<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        ChatResponse response = ragService.chat(request);
        return Result.ok(response);
    }

    /**
     * GET /api/chat/stream?message=xxx&knowledgeBaseId=1
     * 流式 RAG 问答 (SSE)，逐 Token 推送回答内容
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(
            @RequestParam String message,
            @RequestParam(required = false) Long knowledgeBaseId,
            @RequestParam(defaultValue = "5") Integer topK) {

        SseEmitter emitter = new SseEmitter(600_000L);
        AtomicBoolean completed = new AtomicBoolean(false);

        ChatRequest request = new ChatRequest();
        request.setMessage(message);
        request.setKnowledgeBaseId(knowledgeBaseId);
        request.setTopK(topK);

        // 心跳线程：每 15 秒发送 SSE 注释，防止代理/浏览器超时断连
        java.util.concurrent.ScheduledExecutorService heartbeat =
                java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
        heartbeat.scheduleAtFixedRate(() -> {
            if (completed.get()) return;
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (Exception e) {
                // 连接已关闭，忽略
            }
        }, 10, 15, java.util.concurrent.TimeUnit.SECONDS);

        Flux<String> stream = ragService.chatStream(request);

        Disposable disposable = stream
                .doOnNext(chunk -> {
                    if (completed.get()) return;
                    try {
                        emitter.send(SseEmitter.event().data(chunk));
                    } catch (Exception e) {
                        log.warn("SSE 发送失败: {}", e.getMessage());
                        completed.set(true);
                    }
                })
                .doOnComplete(() -> {
                    if (completed.compareAndSet(false, true)) {
                        heartbeat.shutdownNow();
                        try {
                            emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                            emitter.complete();
                        } catch (Exception e) {
                            log.debug("SSE 完成信号发送失败: {}", e.getMessage());
                        }
                    }
                })
                .doOnError(e -> {
                    log.warn("流式输出异常: {}", e.getMessage());
                    if (completed.compareAndSet(false, true)) {
                        heartbeat.shutdownNow();
                        try {
                            emitter.send(SseEmitter.event().data("模型服务响应异常，请稍后重试。"));
                            emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                            emitter.complete();
                        } catch (Exception ex) {
                            emitter.completeWithError(e);
                        }
                    }
                })
                .subscribe();

        emitter.onCompletion(() -> {
            completed.set(true);
            heartbeat.shutdownNow();
            disposable.dispose();
        });
        emitter.onTimeout(() -> {
            completed.set(true);
            heartbeat.shutdownNow();
            disposable.dispose();
            emitter.complete();
        });

        return emitter;
    }
}
