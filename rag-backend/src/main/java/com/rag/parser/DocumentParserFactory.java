package com.rag.parser;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 根据文件扩展名选择合适的 DocumentParser
 */
@Component
@RequiredArgsConstructor
public class DocumentParserFactory {

    private final List<DocumentParser> parsers;

    /**
     * 根据文件扩展名返回对应的解析器
     * @throws IllegalArgumentException 如果没有匹配的解析器
     */
    public DocumentParser getParser(String fileExtension) {
        String ext = fileExtension.toLowerCase();
        return parsers.stream()
                .filter(p -> p.supports(ext))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的文件格式: " + ext));
    }
}
