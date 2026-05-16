package com.rag.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 小说模块专用 LLM 客户端 —— 独立于智能问答的 ChatClient，
 * 使用直接 HTTP + SSE 流式调用，禁用 thinking 以加速生成。
 */
@Slf4j
@Component
public class NovelLlmClient {

    @Value("${rag.novel.ai.api-key:${spring.ai.openai.api-key}}")
    private String apiKey;

    @Value("${rag.novel.ai.base-url:${spring.ai.openai.base-url}}")
    private String baseUrl;

    @Value("${rag.novel.ai.model:${spring.ai.openai.chat.options.model:MiniMax-M2.5}}")
    private String modelName;

    @Value("${rag.novel.ai.temperature:0.7}")
    private double temperature;

    @Value("${rag.novel.ai.max-tokens:4096}")
    private int maxTokens;

    /**
     * 调用 LLM 生成内容（同步流式读取）。
     * 失败时返回友好提示字符串而不抛异常（适用于大纲生成等场景）。
     */
    public String call(String prompt) {
        log.info("NovelLlmClient.call: prompt长度={}", prompt.length());
        try {
            return doCall(prompt);
        } catch (Exception ex) {
            log.warn("LLM 调用异常: {}", ex.getMessage());
            return "AI 生成服务暂时不可用，请稍后重试。";
        }
    }

    /**
     * 调用 LLM 生成内容（同步流式读取）。
     * 失败时抛出 RuntimeException（适用于章节生成等需要重试的场景）。
     */
    public String callOrThrow(String prompt) {
        log.info("NovelLlmClient.callOrThrow: prompt长度={}", prompt.length());
        try {
            return doCall(prompt);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("LLM 调用失败: " + ex.getMessage(), ex);
        }
    }

    /**
     * 流式调用 LLM，逐 chunk 返回 Flux&lt;String&gt;（用于章节实时生成流）。
     * 自动过滤 &lt;think&gt; 标签内容。
     */
    public Flux<String> stream(String prompt) {
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
        new Thread(() -> {
            try {
                String requestBody = buildRequestJson(prompt, temperature);
                HttpClient httpClient = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(30)).build();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/v1/chat/completions"))
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                        .timeout(Duration.ofMinutes(10)).build();

                HttpResponse<java.io.InputStream> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() != 200) {
                    String err = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                    sink.tryEmitError(new RuntimeException("API错误 HTTP " + response.statusCode() + ": " + err));
                    return;
                }

                boolean inThink = false;
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.startsWith("data: ")) continue;
                        String data = line.substring(6).trim();
                        if ("[DONE]".equals(data)) break;
                        String delta = extractDeltaContent(data);
                        if (delta == null || delta.isEmpty()) continue;
                        // 简易 think 标签过滤
                        if (delta.contains("<think>")) { inThink = true; continue; }
                        if (delta.contains("</think>")) { inThink = false; continue; }
                        if (!inThink) {
                            sink.tryEmitNext(delta);
                        }
                    }
                }
                sink.tryEmitComplete();
            } catch (Exception ex) {
                log.error("stream调用失败: {}", ex.getMessage());
                sink.tryEmitError(ex);
            }
        }, "novel-llm-stream").start();
        return sink.asFlux();
    }

    private String doCall(String prompt) throws Exception {
        String requestBody = buildRequestJson(prompt, temperature);
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/chat/completions"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .timeout(Duration.ofMinutes(10))
                .build();

        HttpResponse<java.io.InputStream> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            String errorBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
            throw new RuntimeException("API返回错误 HTTP " + response.statusCode() + ": " + errorBody);
        }

        StringBuilder content = new StringBuilder();
        int chunkCount = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data: ")) continue;
                String data = line.substring(6).trim();
                if ("[DONE]".equals(data)) break;
                String delta = extractDeltaContent(data);
                if (delta != null && !delta.isEmpty()) {
                    content.append(delta);
                    chunkCount++;
                    if (chunkCount <= 3 || chunkCount % 100 == 0) {
                        log.debug("chunk #{}, 累计{}字", chunkCount, content.length());
                    }
                }
            }
        }

        String result = content.toString();
        // 去除模型可能返回的 <think>...</think> 推理标签
        result = result.replaceAll("(?s)<think>.*?</think>", "").trim();
        if (result.isBlank()) {
            throw new RuntimeException("LLM 返回空内容");
        }
        log.info("NovelLlmClient: 完成, {}个chunk, {}字", chunkCount, result.length());
        return result;
    }

    /**
     * 调用 LLM 并指定 temperature（如摘要生成用低温度 0.1）。
     */
    public String callWithTemperature(String prompt, double temp) {
        log.info("NovelLlmClient.callWithTemp: temp={}, prompt长度={}", temp, prompt.length());
        try {
            String requestBody = buildRequestJson(prompt, temp);
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v1/chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .timeout(Duration.ofMinutes(10))
                    .build();

            HttpResponse<java.io.InputStream> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                String errorBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                throw new RuntimeException("API返回错误 HTTP " + response.statusCode() + ": " + errorBody);
            }

            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data: ")) continue;
                    String data = line.substring(6).trim();
                    if ("[DONE]".equals(data)) break;
                    String delta = extractDeltaContent(data);
                    if (delta != null && !delta.isEmpty()) {
                        content.append(delta);
                    }
                }
            }

            String result = content.toString();
            result = result.replaceAll("(?s)<think>.*?</think>", "").trim();
            if (result.isBlank()) {
                throw new RuntimeException("LLM 返回空内容");
            }
            return result;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("LLM 调用失败: " + ex.getMessage(), ex);
        }
    }

    private String buildRequestJson(String prompt, double temp) {
        String escapedPrompt = prompt.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "{" +
                "\"model\":\"" + modelName + "\"," +
                "\"messages\":[{\"role\":\"user\",\"content\":\"" + escapedPrompt + "\"}]," +
                "\"max_tokens\":" + maxTokens + "," +
                "\"temperature\":" + temp + "," +
                "\"stream\":true," +
                "\"thinking\":{\"type\":\"disabled\"}" +
                "}";
    }

    private String extractDeltaContent(String json) {
        int deltaIdx = json.indexOf("\"delta\"");
        if (deltaIdx < 0) return null;
        int contentIdx = json.indexOf("\"content\"", deltaIdx);
        if (contentIdx < 0) return null;
        int colonIdx = json.indexOf(":", contentIdx + 9);
        if (colonIdx < 0) return null;
        int startQuote = json.indexOf("\"", colonIdx + 1);
        if (startQuote < 0) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = startQuote + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                switch (next) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u':
                        if (i + 5 < json.length()) {
                            String hex = json.substring(i + 2, i + 6);
                            sb.append((char) Integer.parseInt(hex, 16));
                            i += 4;
                        }
                        break;
                    default: sb.append(next);
                }
                i++;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
