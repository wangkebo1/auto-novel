package com.rag.parser;

import org.springframework.ai.document.Document;

import java.nio.file.Path;
import java.util.List;

/**
 * 文档解析器接口，不同格式实现各自的文本提取逻辑
 */
public interface DocumentParser {

    /**
     * 判断当前解析器是否支持该文件类型
     */
    boolean supports(String fileExtension);

    /**
     * 解析文件，返回 Spring AI Document 列表（每个 Document 为一个文本段落/章节）
     */
    List<Document> parse(Path filePath) throws Exception;
}
