package com.rag.controller;

import com.rag.dto.*;
import com.rag.service.CharacterExtractService;
import com.rag.service.EpubExportService;
import com.rag.service.NovelGenerationManager;
import com.rag.service.NovelService;
import com.rag.service.RewriteService;
import com.rag.service.SensitiveWordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/novels")
@RequiredArgsConstructor
public class NovelController {

    private final NovelService novelService;
    private final NovelGenerationManager generationManager;
    private final SensitiveWordService sensitiveWordService;
    private final EpubExportService epubExportService;
    private final RewriteService rewriteService;
    private final CharacterExtractService characterExtractService;

    // ==================== 小说 CRUD ====================

    @GetMapping
    public Result<List<NovelResponse>> listNovels() {
        return Result.ok(novelService.listMyNovels());
    }

    @PostMapping
    public Result<NovelResponse> createNovel(@RequestBody NovelRequest request) {
        return Result.ok(novelService.createNovel(request));
    }

    @GetMapping("/{novelId}")
    public Result<NovelResponse> getNovel(@PathVariable Long novelId) {
        return Result.ok(novelService.getNovel(novelId));
    }

    @PutMapping("/{novelId}")
    public Result<NovelResponse> updateNovel(@PathVariable Long novelId, @RequestBody NovelRequest request) {
        return Result.ok(novelService.updateNovel(novelId, request));
    }

    @DeleteMapping("/{novelId}")
    public Result<Void> deleteNovel(@PathVariable Long novelId) {
        novelService.deleteNovel(novelId);
        return Result.ok(null);
    }

    // ==================== 角色管理 ====================

    @PostMapping("/{novelId}/characters")
    public Result<NovelResponse.CharacterInfo> addCharacter(
            @PathVariable Long novelId, @RequestBody CharacterRequest request) {
        return Result.ok(novelService.addCharacter(novelId, request));
    }

    @PutMapping("/{novelId}/characters/{characterId}")
    public Result<NovelResponse.CharacterInfo> updateCharacter(
            @PathVariable Long novelId, @PathVariable Long characterId,
            @RequestBody CharacterRequest request) {
        return Result.ok(novelService.updateCharacter(novelId, characterId, request));
    }

    @PutMapping("/{novelId}/characters/{characterId}/replace")
    public Result<NovelResponse.CharacterInfo> updateCharacterWithReplace(
            @PathVariable Long novelId, @PathVariable Long characterId,
            @RequestParam String oldName, @RequestBody CharacterRequest request) {
        return Result.ok(novelService.updateCharacterWithReplace(novelId, characterId, oldName, request));
    }

    @DeleteMapping("/{novelId}/characters/{characterId}")
    public Result<Void> deleteCharacter(@PathVariable Long novelId, @PathVariable Long characterId) {
        novelService.deleteCharacter(novelId, characterId);
        return Result.ok(null);
    }

    @PostMapping("/{novelId}/extract-characters")
    public Result<List<ExtractedCharacter>> extractCharacters(@PathVariable Long novelId) {
        return Result.ok(characterExtractService.extractCharacters(novelId));
    }

    @DeleteMapping("/{novelId}/chapters/{chapterId}")
    public Result<Void> deleteChapter(@PathVariable Long novelId, @PathVariable Long chapterId) {
        novelService.deleteChapter(novelId, chapterId);
        return Result.ok(null);
    }

    @DeleteMapping("/{novelId}/outline-chapters")
    public Result<Void> deleteOutlineChapters(@PathVariable Long novelId) {
        novelService.deleteOutlineChapters(novelId);
        return Result.ok(null);
    }

    // ==================== AI 生成 ====================

    @PostMapping("/{novelId}/generate-outline")
    public Result<GenerationStatus> generateOutline(
            @PathVariable Long novelId,
            @RequestParam(defaultValue = "10") Integer chapterCount) {
        try {
            return Result.ok(generationManager.startOutlineGeneration(novelId, chapterCount));
        } catch (Exception ex) {
            throw new RuntimeException("生成大纲失败: " + ex.getMessage(), ex);
        }
    }

    @GetMapping("/{novelId}/outline-status")
    public Result<GenerationStatus> getOutlineStatus(@PathVariable Long novelId) {
        return Result.ok(generationManager.getOutlineStatus(novelId));
    }

    /** 开始后台生成所有章节 */
    @PostMapping("/{novelId}/start-generation")
    public Result<GenerationStatus> startGeneration(
            @PathVariable Long novelId,
            @RequestBody(required = false) GenerateRequest request) {
        return Result.ok(generationManager.startGeneration(novelId, request));
    }

    /** 停止生成 */
    @PostMapping("/{novelId}/stop-generation")
    public Result<GenerationStatus> stopGeneration(@PathVariable Long novelId) {
        return Result.ok(generationManager.stopGeneration(novelId));
    }

    /** 获取生成状态 */
    @GetMapping("/{novelId}/generation-status")
    public Result<GenerationStatus> getGenerationStatus(@PathVariable Long novelId) {
        return Result.ok(generationManager.getStatus(novelId));
    }

    // ==================== 章节管理 ====================

    @GetMapping("/{novelId}/chapters/{chapterId}")
    public Result<ChapterResponse> getChapter(@PathVariable Long novelId, @PathVariable Long chapterId) {
        return Result.ok(novelService.getChapter(novelId, chapterId));
    }

    @PutMapping("/{novelId}/chapters/{chapterId}")
    public Result<ChapterResponse> updateChapter(
            @PathVariable Long novelId, @PathVariable Long chapterId,
            @RequestBody ChapterUpdateRequest request) {
        return Result.ok(novelService.updateChapter(novelId, chapterId,
                request.getTitle(), request.getOutline(), request.getContent()));
    }

    @PutMapping("/{novelId}/chapters/{chapterId}/notes")
    public Result<ChapterResponse> updateChapterNotes(
            @PathVariable Long novelId, @PathVariable Long chapterId,
            @RequestBody java.util.Map<String, String> request) {
        return Result.ok(novelService.updateChapterNotes(novelId, chapterId, request.get("notes")));
    }

    @PostMapping("/{novelId}/chapters/{chapterId}/branch-suggestions")
    public Result<List<BranchSuggestionResponse>> suggestChapterBranches(
            @PathVariable Long novelId, @PathVariable Long chapterId) {
        return Result.ok(novelService.suggestChapterBranches(novelId, chapterId));
    }

    @PostMapping("/{novelId}/chapters/{chapterId}/generate")
    public Result<ChapterResponse> generateChapter(
            @PathVariable Long novelId, @PathVariable Long chapterId,
            @RequestBody(required = false) GenerateRequest request) {
        return Result.ok(novelService.generateChapter(novelId, chapterId, request));
    }

    @GetMapping(value = "/{novelId}/chapters/{chapterId}/generate-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generateChapterStream(
            @PathVariable Long novelId, @PathVariable Long chapterId,
            @RequestParam(required = false) Integer targetWords,
            @RequestParam(required = false) String userPrompt) {

        SseEmitter emitter = new SseEmitter(300_000L);

        GenerateRequest request = new GenerateRequest();
        request.setTargetWords(targetWords);
        request.setUserPrompt(userPrompt);

        Flux<String> stream = novelService.generateChapterStream(novelId, chapterId, request);

        stream
                .doOnNext(chunk -> {
                    try {
                        emitter.send(SseEmitter.event().data(chunk));
                    } catch (Exception e) {
                        log.warn("SSE 发送失败", e);
                        emitter.completeWithError(e);
                    }
                })
                .doOnComplete(() -> {
                    try {
                        emitter.send(SseEmitter.event().data("[DONE]"));
                        emitter.complete();
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                })
                .doOnError(emitter::completeWithError)
                .subscribe();

        return emitter;
    }

    // ==================== 小说统计 ====================

    @GetMapping("/{novelId}/statistics")
    public Result<NovelStatistics> getStatistics(@PathVariable Long novelId) {
        return Result.ok(novelService.getStatistics(novelId));
    }

    @GetMapping("/{novelId}/word-frequency")
    public Result<List<WordFrequency>> getWordFrequency(@PathVariable Long novelId) {
        return Result.ok(novelService.getWordFrequency(novelId));
    }

    @PostMapping("/check-sensitive")
    public Result<List<String>> checkSensitive(@RequestBody java.util.Map<String, String> request) {
        String content = request.get("content");
        return Result.ok(sensitiveWordService.detectSensitiveWords(content));
    }

    @PostMapping("/rewrite")
    public Result<String> rewrite(@RequestBody RewriteRequest request) {
        return Result.ok(rewriteService.rewrite(request));
    }

    // ==================== 小说导出 ====================

    @GetMapping("/{novelId}/export")
    public void exportNovel(
            @PathVariable Long novelId,
            @RequestParam(defaultValue = "txt") String format,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {

        log.info("导出小说: novelId={}, format={}", novelId, format);

        byte[] bytes;
        String ext;
        String contentType;

        if ("epub".equalsIgnoreCase(format)) {
            bytes = epubExportService.exportNovelAsEpub(novelId);
            ext = ".epub";
            contentType = "application/epub+zip";
        } else if ("markdown".equalsIgnoreCase(format) || "md".equalsIgnoreCase(format)) {
            String content = novelService.exportNovelAsMarkdown(novelId);
            bytes = content.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            ext = ".md";
            contentType = "application/octet-stream";
        } else {
            String content = novelService.exportNovelAsText(novelId);
            bytes = content.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            ext = ".txt";
            contentType = "application/octet-stream";
        }

        String fileName = java.net.URLEncoder.encode("novel_" + novelId + ext, java.nio.charset.StandardCharsets.UTF_8);

        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
        response.getOutputStream().flush();
    }

    /** 章节更新请求 */
    @lombok.Data
    public static class ChapterUpdateRequest {
        private String title;
        private String outline;
        private String content;
    }
}
