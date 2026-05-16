package com.rag.service;

import com.rag.dto.ChatRequest;
import com.rag.dto.ChatResponse;
import reactor.core.publisher.Flux;

public interface RagService {

    /**
     * RAG 问答（同步）
     * 流程：向量检索 → Prompt 增强 → LLM 生成
     */
    ChatResponse chat(ChatRequest request);

    /**
     * RAG 流式问答（SSE）
     * 返回 Flux<String>，逐 Token 推送
     */
    Flux<String> chatStream(ChatRequest request);
}
