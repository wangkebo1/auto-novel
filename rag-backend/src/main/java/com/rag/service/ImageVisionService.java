package com.rag.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.rag.dto.ChatRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ImageVisionService {

    private final RestTemplate restTemplate;
    private final String model;
    private final boolean enabled;

    public ImageVisionService(
            RestTemplateBuilder builder,
            @Value("${rag.chat-vision.base-url:https://meta.tangarcin.de}") String baseUrl,
            @Value("${rag.chat-vision.api-key:sk-hGEGgIXrfXqT6Y4Kp}") String apiKey,
            @Value("${rag.chat-vision.model:gpt-5.4}") String model,
            @Value("${rag.chat-vision.timeout-seconds:30}") int timeoutSeconds) {

        this.model = model;
        this.enabled = StringUtils.hasText(baseUrl) && StringUtils.hasText(apiKey) && StringUtils.hasText(model);
        if (enabled) {
            this.restTemplate = builder
                    .rootUri(baseUrl)
                    .setConnectTimeout(Duration.ofSeconds(timeoutSeconds))
                    .setReadTimeout(Duration.ofSeconds(timeoutSeconds))
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();
        } else {
            this.restTemplate = builder.build();
        }
    }

    public String describeImage(ChatRequest.ChatImage image, String question) {
        if (!enabled || image == null || !StringUtils.hasText(image.getBase64())) {
            return "";
        }

        String prompt = StringUtils.hasText(question) ? question.trim() : "请描述下面这张图片的内容";

        List<Object> inputEntries = List.of(
                Map.of(
                        "role", "system",
                        "content", List.of(
                                Map.of("type", "input_text", "text",
                                        "你已经看到了用户上传的图片，不要再询问是否收到了文件，直接在提供的信息基础上分析图像内容。")
                        )
                ),
                Map.of(
                        "role", "user",
                        "content", List.of(
                                Map.of("type", "input_text", "text", "请结合下面这张图片和问题作答： " + prompt),
                                Map.of("type", "input_image", "image", buildDataUrl(image))
                        )
                )
        );

        Map<String, Object> payload = Map.of(
                "model", model,
                "input", inputEntries
        );

        try {
            JsonNode response = restTemplate.postForObject("/v1/responses", payload, JsonNode.class);
            if (response != null && log.isDebugEnabled()) {
                log.debug("图像识别模型响应: {}", response.toPrettyString());
            }
            return extractText(response);
        } catch (Exception ex) {
            log.warn("调用图像识别模型失败: {}", ex.getMessage());
            return "";
        }
    }

    private String buildDataUrl(ChatRequest.ChatImage image) {
        String raw = image.getBase64();
        if (raw == null) {
            return null;
        }
        if (raw.startsWith("data:")) {
            return raw;
        }
        String mime = StringUtils.hasText(image.getMimeType()) ? image.getMimeType() : "image/png";
        return "data:" + mime + ";base64," + raw;
    }

    private String extractText(JsonNode response) {
        if (response == null) {
            return "";
        }
        String outputText = response.path("output_text").asText(null);
        if (StringUtils.hasText(outputText)) {
            String sanitized = sanitizeResponse(outputText);
            if (StringUtils.hasText(sanitized)) {
                return sanitized;
            }
        }

        JsonNode output = response.path("output");
        if (output.isArray()) {
            for (JsonNode item : output) {
                JsonNode content = item.path("content");
                if (content.isArray()) {
                    for (JsonNode piece : content) {
                        String text = piece.path("text").asText(null);
                        if (StringUtils.hasText(text)) {
                            String sanitized = sanitizeResponse(text);
                            if (StringUtils.hasText(sanitized)) {
                                return sanitized;
                            }
                        }
                        String description = piece.path("description").asText(null);
                        if (StringUtils.hasText(description)) {
                            String sanitized = sanitizeResponse(description);
                            if (StringUtils.hasText(sanitized)) {
                                return sanitized;
                            }
                        }
                    }
                }
            }
        }
        return response.path("error").path("message").asText("");
    }

    private String sanitizeResponse(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String normalized = text.trim();
        List<String> blockers = List.of("请上传", "请把图片", "还没有看到", "无法分析");
        String cleaned = normalized;
        boolean hadBlocker = false;
        for (String blocker : blockers) {
            if (cleaned.contains(blocker)) {
                cleaned = cleaned.replace(blocker, "");
                hadBlocker = true;
            }
        }
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        if (StringUtils.hasText(cleaned)) {
            return cleaned;
        }
        if (hadBlocker && !normalized.isBlank()) {
            log.debug("图像识别响应仅剩阻塞信息，原始内容: {}", normalized);
        }
        return "";
    }
}
