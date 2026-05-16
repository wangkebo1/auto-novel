package com.rag.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.*;

/**
 * Word 文档解析器 (.doc / .docx)
 * - .docx: 按段落/标题结构化提取，表格转 Markdown
 * - .doc:  整体文本提取
 */
@Slf4j
@Component
public class WordDocumentParser implements DocumentParser {

    private static final Set<String> SUPPORTED = Set.of("doc", "docx");

    @Override
    public boolean supports(String fileExtension) {
        return SUPPORTED.contains(fileExtension.toLowerCase());
    }

    @Override
    public List<Document> parse(Path filePath) throws Exception {
        String ext = getExtension(filePath);
        if ("docx".equals(ext)) {
            return parseDocx(filePath);
        } else {
            return parseDoc(filePath);
        }
    }

    /**
     * 解析 .docx —— 按标题分段，表格转 Markdown 格式
     */
    private List<Document> parseDocx(Path filePath) throws Exception {
        List<Document> documents = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             XWPFDocument doc = new XWPFDocument(fis)) {

            StringBuilder currentSection = new StringBuilder();
            String currentHeading = "";

            for (IBodyElement element : doc.getBodyElements()) {
                if (element instanceof XWPFParagraph para) {
                    String style = para.getStyleID();
                    String text = para.getText().trim();

                    if (text.isEmpty()) continue;

                    // 检测标题段落（Heading1~Heading9）
                    if (isHeading(style)) {
                        // 前一段内容存为一个 Document
                        flushSection(documents, currentSection, currentHeading);
                        currentHeading = text;
                        currentSection = new StringBuilder();
                    } else {
                        currentSection.append(text).append("\n");
                    }
                } else if (element instanceof XWPFTable table) {
                    // 表格转 Markdown 格式，保留结构信息
                    String mdTable = tableToMarkdown(table);
                    currentSection.append(mdTable).append("\n");
                }
            }
            // 最后一段
            flushSection(documents, currentSection, currentHeading);
        }

        log.info("Word(.docx) 解析完成: {}，共 {} 个章节", filePath.getFileName(), documents.size());
        return documents;
    }

    /**
     * 解析 .doc（旧版 Word 97-2003）
     */
    private List<Document> parseDoc(Path filePath) throws Exception {
        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             HWPFDocument doc = new HWPFDocument(fis)) {
            WordExtractor extractor = new WordExtractor(doc);
            String text = extractor.getText().trim();
            if (text.isEmpty()) {
                return Collections.emptyList();
            }
            Document document = new Document(text, Map.of("format", "doc"));
            log.info("Word(.doc) 解析完成: {}，文本长度: {}", filePath.getFileName(), text.length());
            return List.of(document);
        }
    }

    private void flushSection(List<Document> documents, StringBuilder section, String heading) {
        String content = section.toString().trim();
        if (content.isEmpty()) return;

        // 如果有标题，将标题拼在段落前面
        String fullText = heading.isEmpty() ? content : "## " + heading + "\n" + content;
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("format", "docx");
        if (!heading.isEmpty()) {
            metadata.put("heading", heading);
        }
        documents.add(new Document(fullText, metadata));
    }

    /**
     * 将 Word 表格转为 Markdown 格式，保留行列结构
     */
    private String tableToMarkdown(XWPFTable table) {
        List<XWPFTableRow> rows = table.getRows();
        if (rows.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rows.size(); i++) {
            XWPFTableRow row = rows.get(i);
            sb.append("| ");
            for (XWPFTableCell cell : row.getTableCells()) {
                sb.append(cell.getText().trim().replace("|", "\\|")).append(" | ");
            }
            sb.append("\n");

            // 第一行后加分隔线（Markdown 表头）
            if (i == 0) {
                sb.append("| ");
                for (int j = 0; j < row.getTableCells().size(); j++) {
                    sb.append("--- | ");
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private boolean isHeading(String styleId) {
        if (styleId == null) return false;
        // Word 标题样式: Heading1, Heading2, ... 或中文 "标题 1"
        return styleId.matches("(?i)heading\\d+|.*标题.*");
    }

    private String getExtension(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1).toLowerCase() : "";
    }
}
