package com.rag.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.DateUtil;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.*;

/**
 * Excel 文档解析器 (.xls / .xlsx)
 * - 逐 Sheet 解析，保留表头行列结构
 * - 每个 Sheet 输出为 Markdown 表格格式的 Document
 * - 空行自动跳过，大 Sheet 分段输出（避免单个 chunk 过长）
 */
@Slf4j
@Component
public class ExcelDocumentParser implements DocumentParser {

    private static final Set<String> SUPPORTED = Set.of("xls", "xlsx");
    /** 每个分段最大行数，超过此阈值则分为多个 Document */
    private static final int MAX_ROWS_PER_CHUNK = 50;

    @Override
    public boolean supports(String fileExtension) {
        return SUPPORTED.contains(fileExtension.toLowerCase());
    }

    @Override
    public List<Document> parse(Path filePath) throws Exception {
        List<Document> documents = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             Workbook workbook = WorkbookFactory.create(fis)) {

            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                Sheet sheet = workbook.getSheetAt(s);
                String sheetName = sheet.getSheetName();
                List<Document> sheetDocs = parseSheet(sheet, sheetName);
                documents.addAll(sheetDocs);
            }
        }

        log.info("Excel 解析完成: {}，共 {} 个文档段", filePath.getFileName(), documents.size());
        return documents;
    }

    private List<Document> parseSheet(Sheet sheet, String sheetName) {
        List<Document> documents = new ArrayList<>();
        int lastRowNum = sheet.getLastRowNum();
        if (lastRowNum < 0) return documents;

        // 读取表头（第一行）
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) return documents;
        List<String> headers = readRow(headerRow);
        if (headers.stream().allMatch(String::isEmpty)) return documents;

        int colCount = headers.size();
        String mdHeader = buildMdHeader(headers);

        // 分段：每 MAX_ROWS_PER_CHUNK 行数据为一个 Document
        StringBuilder currentChunk = new StringBuilder();
        currentChunk.append(mdHeader);
        int rowsInChunk = 0;
        int chunkIndex = 1;

        for (int r = 1; r <= lastRowNum; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            List<String> cells = readRow(row, colCount);
            // 跳过全空行
            if (cells.stream().allMatch(String::isEmpty)) continue;

            currentChunk.append("| ");
            for (String cell : cells) {
                currentChunk.append(cell.replace("|", "\\|")).append(" | ");
            }
            currentChunk.append("\n");
            rowsInChunk++;

            if (rowsInChunk >= MAX_ROWS_PER_CHUNK) {
                documents.add(createDocument(currentChunk, sheetName, chunkIndex));
                chunkIndex++;
                rowsInChunk = 0;
                currentChunk = new StringBuilder();
                currentChunk.append(mdHeader); // 每个分段重复表头
            }
        }

        // 最后一段
        if (rowsInChunk > 0) {
            documents.add(createDocument(currentChunk, sheetName, chunkIndex));
        }

        return documents;
    }

    private Document createDocument(StringBuilder content, String sheetName, int chunkIndex) {
        String text = "### Sheet: " + sheetName + "\n" + content.toString().trim();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("format", "excel");
        metadata.put("sheetName", sheetName);
        metadata.put("chunkIndex", String.valueOf(chunkIndex));
        return new Document(text, metadata);
    }

    private String buildMdHeader(List<String> headers) {
        StringBuilder sb = new StringBuilder();
        sb.append("| ");
        for (String h : headers) {
            sb.append(h.replace("|", "\\|")).append(" | ");
        }
        sb.append("\n| ");
        for (int i = 0; i < headers.size(); i++) {
            sb.append("--- | ");
        }
        sb.append("\n");
        return sb.toString();
    }

    private List<String> readRow(Row row) {
        return readRow(row, row.getLastCellNum());
    }

    private List<String> readRow(Row row, int colCount) {
        List<String> cells = new ArrayList<>();
        for (int c = 0; c < colCount; c++) {
            Cell cell = row.getCell(c, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cells.add(getCellValue(cell));
        }
        return cells;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double val = cell.getNumericCellValue();
                // 整数不显示小数点
                yield val == Math.floor(val) ? String.valueOf((long) val) : String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    try {
                        yield cell.getStringCellValue();
                    } catch (Exception e2) {
                        yield cell.getCellFormula();
                    }
                }
            }
            default -> "";
        };
    }
}
