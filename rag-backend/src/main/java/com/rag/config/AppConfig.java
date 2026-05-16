package com.rag.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.ollama.management.ModelManagementOptions;

import java.io.File;

@Slf4j
@Configuration
public class AppConfig {

    @Value("${rag.upload.path:./uploads}")
    private String uploadPath;

    /**
     * 全局 CORS 配置，允许前端跨域请求
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOriginPatterns("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(false)
                        .maxAge(3600);
            }
        };
    }

    /**
     * Ollama Embedding Model（用于向量化，Chat 仍走 edgefn.net）
     */
    @Bean
    @Primary
    public EmbeddingModel embeddingModel() {
        OllamaApi ollamaApi = new OllamaApi("http://localhost:11434");
        OllamaOptions options = OllamaOptions.builder().model("nomic-embed-text").build();
        return new OllamaEmbeddingModel(ollamaApi, options,
                ObservationRegistry.NOOP, ModelManagementOptions.defaults());
    }

    /**
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("你是一个专业的企业知识库助手，请基于提供的上下文内容准确回答用户问题。如果上下文中没有相关信息，请明确告知用户。")
                .build();
    }

    /**
     * 应用启动初始化：
     * 1. 创建 pgvector 扩展（需要 PostgreSQL 超级用户权限）
     * 2. 确保文件上传目录存在
     */
    @Bean
    public ApplicationRunner initApplication(JdbcTemplate jdbcTemplate) {
        return args -> {
            // 初始化 pgvector 扩展
            try {
                jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
                log.info("PGVector 扩展初始化成功");
            } catch (Exception e) {
                log.warn("PGVector 扩展创建失败（可能已存在或权限不足）: {}", e.getMessage());
            }

            // 创建上传目录
            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists() && uploadDir.mkdirs()) {
                log.info("文件上传目录已创建: {}", uploadDir.getAbsolutePath());
            }

            log.info("RAG 知识库问答系统启动完成，文件上传路径: {}", uploadDir.getAbsolutePath());
        };
    }
}
