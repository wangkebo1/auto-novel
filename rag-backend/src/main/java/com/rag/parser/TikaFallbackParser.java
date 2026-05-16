package com.rag.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * 通用 Tika 解析器：处理 PDF / TXT / Markdown 等格式
 * 作为兜底解析器，支持所有 Word/Excel 之外的格式
 */
@Slf4j
@Component
public class TikaFallbackParser implements DocumentParser {

    private static final Set<String> SUPPORTED = Set.of("pdf", "txt", "md", "html", "rtf");

    @Override
    public boolean supports(String fileExtension) {
        return SUPPORTED.contains(fileExtension.toLowerCase());
    }

    @Override
    public List<Document> parse(Path filePath) throws Exception {
        TikaDocumentReader reader = new TikaDocumentReader(new FileSystemResource(filePath));
        List<Document> documents = reader.get();
        log.info("Tika 解析完成: {}，段落数: {}", filePath.getFileName(), documents.size());
        return documents;
    }
}
