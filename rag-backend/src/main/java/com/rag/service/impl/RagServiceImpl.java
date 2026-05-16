package com.rag.service.impl;

import com.rag.dto.ChatRequest;
import com.rag.dto.ChatResponse;
import com.rag.service.ImageVisionService;
import com.rag.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final EmbeddingModel embeddingModel;
    private final JdbcTemplate jdbcTemplate;
    private final ImageVisionService imageVisionService;

    @Value("${rag.retrieval.top-k:5}")
    private int defaultTopK;

    @Value("${rag.retrieval.similarity-threshold:0.5}")
    private double similarityThreshold;

    @Value("${rag.llm.timeout-seconds:45}")
    private long llmTimeoutSeconds;

    /**
     * RAG Prompt 模板：
     * - 注入检索到的上下文
     * - 引导模型基于上下文回答，避免幻觉
     */
    private static final String RAG_PROMPT_TEMPLATE = """
            请严格基于以下上下文内容回答用户问题。
            如果上下文中包含可用信息，必须先基于上下文给出回答，不要直接输出"根据知识库中的文档，暂时无法找到相关信息"这类兜底句。
            只有在上下文为空或与问题明显无关时，才允许输出该兜底句。
            请简洁直接地回答，不要过度分析或长时间思考。
            
            ===== 上下文开始 =====
            {context}
            ===== 上下文结束 =====
            
            用户问题：{question}
            
            请用中文简洁回答：
            """;
    private static final String IMAGE_STANDALONE_PROMPT = """
            你是一个擅长观察图像细节并直接作答的智能助手，而且你已经看到了当前上传的图片。
            因此请立即从图中提炼有效信息并回答，下文中不要再问“有没有看到图片”或要用户重新上传。

            如果用户问题中包含与当前画面不符的术语（比如“乳腺超声”等），请明确说明你观察到的实际内容，并以图片为准来判断，只在最后附带一句“注意：原问题提到…但图像……”，不要把原先错误的语义当作事实。

            结合图片内容，回答以下提问：%s

            直接列出图中可观测的要素、构图、人物、动作、情绪或潜在意图，并说明你的推理路径。
            如果信息不足以判定某点，就告诉用户还需要哪类额外信息。
            """;

    @Override
    public ChatResponse chat(ChatRequest request) {
        String enrichedQuestion = resolveQuestion(request);
        log.info("收到问答请求: {}, kbId={}", enrichedQuestion, request.getKnowledgeBaseId());

        if (request.getImage() != null) {
            String standalonePrompt = String.format(IMAGE_STANDALONE_PROMPT, enrichedQuestion);
            String answer = generateAnswer(standalonePrompt);
            return ChatResponse.builder()
                    .answer(answer)
                    .sources(List.of())
                    .chunks(List.of())
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        // 1. 向量检索：将用户问题转为 Embedding，搜索最相关的文本块
        List<Document> relevantDocs = retrieveRelevantDocs(request);

        if (relevantDocs.isEmpty()) {
            String standalonePrompt = String.format(IMAGE_STANDALONE_PROMPT, enrichedQuestion);
            String answer = generateAnswer(standalonePrompt);
            return ChatResponse.builder()
                    .answer(answer)
                    .sources(List.of())
                    .chunks(List.of())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
        // 2. 拼接上下文（多个文本块用分隔线分开）
        String context = buildContext(relevantDocs);

        // 3. 构建增强 Prompt
        String augmentedPrompt = RAG_PROMPT_TEMPLATE
                .replace("{context}", context)
                .replace("{question}", request.getMessage());

        // 4. 显式超时控制，避免上游模型无响应导致接口长期挂起
        String answer = generateAnswer(augmentedPrompt);

        if (answer == null || answer.isBlank()) {
            answer = "根据知识库中的文档，暂时无法找到与您问题相关的信息。请尝试换一种问法，或先上传相关文档。";
        }

        log.info("LLM 回答生成完毕，参考文档数: {}", relevantDocs.size());

        // 5. 整理来源信息
        List<String> sources = relevantDocs.stream()
                .map(doc -> (String) doc.getMetadata().getOrDefault("fileName", "未知文档"))
                .distinct()
                .collect(Collectors.toList());

        List<ChatResponse.SourceChunk> sourceChunks = relevantDocs.stream()
                .map(doc -> ChatResponse.SourceChunk.builder()
                        .content(doc.getText())
                        .fileName((String) doc.getMetadata().getOrDefault("fileName", "未知文档"))
                        .score(doc.getMetadata().containsKey("distance")
                                ? 1.0 - (Double) doc.getMetadata().get("distance") : null)
                        .build())
                .collect(Collectors.toList());

        return ChatResponse.builder()
                .answer(answer)
                .sources(sources)
                .chunks(sourceChunks)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public Flux<String> chatStream(ChatRequest request) {
        String enrichedQuestion = resolveQuestion(request);
        log.info("收到流式问答请求: {}, kbId={}", enrichedQuestion, request.getKnowledgeBaseId());

        if (request.getImage() != null) {
            String standalonePrompt = String.format(IMAGE_STANDALONE_PROMPT, enrichedQuestion);
            return streamPrompt(standalonePrompt);
        }

        List<Document> relevantDocs = retrieveRelevantDocs(request);

        if (relevantDocs.isEmpty()) {
            String standalonePrompt = String.format(IMAGE_STANDALONE_PROMPT, enrichedQuestion);
            return streamPrompt(standalonePrompt);
        }

        String context = buildContext(relevantDocs);
        String augmentedPrompt = RAG_PROMPT_TEMPLATE
                .replace("{context}", context)
                .replace("{question}", request.getMessage());

        // 使用 ChatClient 的流式 API，逐 Token 推送
        // 不在后端过滤 <think> 标签，保持 SSE 连接活跃，前端负责过滤显示
        return chatClient.prompt()
                .user(augmentedPrompt)
                .stream()
                .content()
                .filter(chunk -> chunk != null && !chunk.isEmpty())
                .timeout(Duration.ofSeconds(llmTimeoutSeconds))
                .onErrorResume(e -> {
                    log.warn("流式 LLM 调用失败: {}", e.getMessage());
                    return Flux.just("模型服务当前响应超时，请稍后重试。");
                });
    }

    private Flux<String> streamPrompt(String prompt) {
        return chatClient.prompt()
                .user(prompt)
                .stream()
                .content()
                .filter(chunk -> chunk != null && !chunk.isEmpty())
                .timeout(Duration.ofSeconds(llmTimeoutSeconds))
                .onErrorResume(e -> {
                    log.warn("流式 LLM 调用失败: {}", e.getMessage());
                    return Flux.just("模型服务当前响应超时，请稍后重试。");
                });
    }

    // ——————————————————————— 私有工具方法 ———————————————————————

    /**
     * 向量检索：构建 SearchRequest 并执行相似度搜索
     */
    private String resolveQuestion(ChatRequest request) {
        boolean hasImage = request.getImage() != null;
        String text = request.getMessage() != null ? request.getMessage().trim() : "";
        if (!StringUtils.hasText(text) && !hasImage) {
            throw new IllegalArgumentException("Please enter a question or upload an image first.");
        }

        String imageDescription = "";
        if (hasImage) {
            imageDescription = imageVisionService.describeImage(request.getImage(), text);
        }

        String finalText;
        if (hasImage && StringUtils.hasText(imageDescription)) {
            finalText = "[附图描述]\n" + imageDescription.trim();
            if (StringUtils.hasText(text)) {
                finalText += "\n[原始问题]\n" + text;
            }
        } else if (hasImage) {
            finalText = StringUtils.hasText(text) ? text : "请描述图片内容";
            finalText += "\n[Image notes] 图片已上传，请直接分析其视觉内容。";
            log.warn("图像识别未返回描述，将使用默认提示继续处理，kbId={}, question={}", request.getKnowledgeBaseId(), finalText);
        } else {
            finalText = text;
        }

        request.setMessage(finalText);
        return finalText;
    }

    private List<Document> retrieveRelevantDocs(ChatRequest request) {
        int topK = request.getTopK() != null ? request.getTopK() : defaultTopK;

        // 获取查询文本的向量表示
        float[] queryEmbedding = embeddingModel.embed(request.getMessage());
        String vectorStr = toVectorString(queryEmbedding);

        String sql;
        Object[] params;

        if (request.getKnowledgeBaseId() != null) {
            sql = "SELECT id, content, metadata, 1 - (embedding <=> ?::vector) AS score " +
                  "FROM vector_store WHERE metadata->>'knowledgeBaseId' = ? " +
                  "ORDER BY embedding <=> ?::vector LIMIT ?";
            params = new Object[]{vectorStr, String.valueOf(request.getKnowledgeBaseId()), vectorStr, topK};
        } else {
            sql = "SELECT id, content, metadata, 1 - (embedding <=> ?::vector) AS score " +
                  "FROM vector_store ORDER BY embedding <=> ?::vector LIMIT ?";
            params = new Object[]{vectorStr, vectorStr, topK};
        }

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            String content = rs.getString("content");
            String metadataJson = rs.getString("metadata");
            double score = rs.getDouble("score");

            Map<String, Object> metadata = new HashMap<>();
            if (metadataJson != null && !metadataJson.isEmpty()) {
                try {
                    var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    metadata = mapper.readValue(metadataJson, Map.class);
                } catch (Exception e) {
                    log.warn("解析 metadata 失败", e);
                }
            }
            metadata.put("score", score);

            return new Document(rs.getString("id"), content, metadata);
        });
    }

    private String toVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    private String generateAnswer(String augmentedPrompt) {
        try {
            return CompletableFuture
                    .supplyAsync(() -> chatClient.prompt().user(augmentedPrompt).call().content())
                    .orTimeout(llmTimeoutSeconds, TimeUnit.SECONDS)
                    .exceptionally(ex -> {
                        log.warn("LLM 调用超时或失败: {}", ex.getMessage());
                        return "模型服务当前响应超时，请稍后重试。您也可以简化问题或降低 Top-K 后再试。";
                    })
                    .join();
        } catch (Exception ex) {
            log.warn("LLM 调用异常: {}", ex.getMessage());
            return "模型服务当前不可用，请稍后重试。";
        }
    }

    /**
     * 将多个文档片段拼接为上下文字符串
     */
    private String buildContext(List<Document> docs) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < docs.size(); i++) {
            Document doc = docs.get(i);
            String fileName = (String) doc.getMetadata().getOrDefault("fileName", "未知文档");
            sb.append("[文档片段 ").append(i + 1).append(" - 来源: ").append(fileName).append("]\n");
            sb.append(doc.getText()).append("\n\n");
        }
        return sb.toString().trim();
    }
}
