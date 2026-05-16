package com.rag.service;

import com.rag.dto.GenerateRequest;
import com.rag.dto.GenerationStatus;
import com.rag.entity.Chapter;
import com.rag.entity.Novel;
import com.rag.entity.NovelCharacter;
import com.rag.repository.ChapterRepository;
import com.rag.repository.NovelCharacterRepository;
import com.rag.repository.NovelRepository;
import com.rag.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.*;

/**
 * 小说后台生成管理器 —— 脱离 HTTP 连接，后台持续逐章生成
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NovelGenerationManager {

    private final NovelRepository novelRepository;
    private final ChapterRepository chapterRepository;
    private final NovelCharacterRepository characterRepository;
    private final NovelLlmClient novelLlmClient;
    private final BillingService billingService;
    private final SecurityUtils securityUtils;

    @Value("${rag.llm.timeout-seconds:45}")
    private long llmTimeoutSeconds;

    /** 活跃的生成任务 key=novelId */
    private final ConcurrentHashMap<Long, TaskInfo> activeTasks = new ConcurrentHashMap<>();
    /** 活跃的大纲生成任务 key=novelId */
    private final ConcurrentHashMap<Long, OutlineTaskInfo> activeOutlineTasks = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    // ==================== 公共接口 ====================

    /**
     * 开始后台生成大纲
     */
    public GenerationStatus startOutlineGeneration(Long novelId, Integer chapterCount) {
        Long userId = securityUtils.getCurrentUserId();
        novelRepository.findByIdAndUserId(novelId, userId)
                .orElseThrow(() -> new RuntimeException("小说不存在或无权访问"));

        int effectiveCount = chapterCount != null ? chapterCount : 10;

        OutlineTaskInfo existing = activeOutlineTasks.get(novelId);
        if (existing != null) {
            GenerationStatus status = getOutlineStatus(novelId);
            if ("RUNNING".equals(status.getStatus())) {
                return status;
            }
            activeOutlineTasks.remove(novelId);
        }

        // Charge happens after outline chapters are successfully saved (see runOutlineGeneration),
        // to avoid consuming points when generation/parsing fails.

        OutlineTaskInfo task = new OutlineTaskInfo();
        task.status = "RUNNING";
        task.novelId = novelId;
        task.userId = userId;
        task.chapterCount = effectiveCount;
        task.totalChapters = effectiveCount;
        task.completedChapters = 0;
        task.stopped = false;
        task.message = "正在生成大纲...";

        activeOutlineTasks.put(novelId, task);
        task.future = executor.submit(() -> runOutlineGeneration(task));

        GenerationStatus gs = new GenerationStatus();
        gs.setStatus("RUNNING");
        gs.setMessage("正在生成大纲...");
        return gs;
    }

    /**
     * 获取大纲生成状态
     */
    public GenerationStatus getOutlineStatus(Long novelId) {
        GenerationStatus gs = new GenerationStatus();
        OutlineTaskInfo task = activeOutlineTasks.get(novelId);

        if (task == null) {
            gs.setStatus("IDLE");
            gs.setMessage("未在生成中");
            return gs;
        }

        gs.setStatus(task.status);
        gs.setMessage(task.message);
        gs.setTotalChapters(task.totalChapters);
        gs.setCompletedChapters(task.completedChapters);
        return gs;
    }

    /**
     * 开始后台生成所有未完成章节
     */
    public GenerationStatus startGeneration(Long novelId, GenerateRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        // 校验权限
        novelRepository.findByIdAndUserId(novelId, userId)
                .orElseThrow(() -> new RuntimeException("小说不存在或无权访问"));

        if (activeTasks.containsKey(novelId)) {
            GenerationStatus status = getStatus(novelId);
            if ("RUNNING".equals(status.getStatus())) {
                return status; // 已在运行
            }
        }

        List<Chapter> chapters = chapterRepository.findByNovelIdOrderByChapterNumberAsc(novelId);
        int pendingChapters = (int) chapters.stream().filter(ch -> !isChapterDone(ch.getStatus())).count();
        if (pendingChapters == 0) {
            throw new RuntimeException("没有待生成的章节");
        }

        int reservedCost = pendingChapters * BillingCosts.CHAPTER;
        billingService.consumePoints(
                userId,
                reservedCost,
                "CHAPTER_BATCH",
                novelId,
                "Continuous generation pre-charge (" + pendingChapters + " 章)"
        );
        log.info("预扣连续创作点数: {} 点（{} 章），novelId={}", reservedCost, pendingChapters, novelId);

        int targetWords = (request != null && request.getTargetWords() != null) ? request.getTargetWords() : 2000;

        TaskInfo task = new TaskInfo();
        task.status = "RUNNING";
        task.novelId = novelId;
        task.userId = userId;
        task.targetWords = targetWords;
        task.stopped = false;
        task.preChargedChapters = pendingChapters;

        activeTasks.put(novelId, task);

        // 提交后台任务
        task.future = executor.submit(() -> runGeneration(task));

        GenerationStatus gs = new GenerationStatus();
        gs.setStatus("RUNNING");
        gs.setMessage("开始生成");
        return gs;
    }

    /**
     * 停止生成
     */
    public GenerationStatus stopGeneration(Long novelId) {
        Long userId = securityUtils.getCurrentUserId();
        novelRepository.findByIdAndUserId(novelId, userId)
                .orElseThrow(() -> new RuntimeException("小说不存在或无权访问"));

        TaskInfo task = activeTasks.get(novelId);
        if (task != null) {
            task.stopped = true;
            task.status = "STOPPED";
            task.message = "用户停止了生成";
        }

        GenerationStatus gs = new GenerationStatus();
        gs.setStatus("STOPPED");
        gs.setMessage("已停止生成");
        return gs;
    }

    /**
     * 获取生成状态
     */
    public GenerationStatus getStatus(Long novelId) {
        GenerationStatus gs = new GenerationStatus();

        TaskInfo task = activeTasks.get(novelId);
        if (task == null) {
            // 没在活跃任务中，检查数据库中是否有 GENERATING 状态的章节
            List<Chapter> chapters = chapterRepository.findByNovelIdOrderByChapterNumberAsc(novelId);
            long total = chapters.size();
            long completed = chapters.stream().filter(c ->
                    "GENERATED".equals(c.getStatus()) || "EDITED".equals(c.getStatus())).count();

            gs.setStatus(completed == total && total > 0 ? "COMPLETED" : "IDLE");
            gs.setTotalChapters((int) total);
            gs.setCompletedChapters((int) completed);
            gs.setMessage(gs.getStatus().equals("COMPLETED") ? "全部章节已生成完成" : "未在生成中");
            return gs;
        }

        gs.setStatus(task.status);
        gs.setCurrentChapter(task.currentChapter);
        gs.setTotalChapters(task.totalChapters);
        gs.setCompletedChapters(task.completedChapters);
        gs.setCurrentChapterTitle(task.currentChapterTitle);
        gs.setMessage(task.message);
        return gs;
    }

    // ==================== 后台生成逻辑 ====================

    private void runGeneration(TaskInfo task) {
        try {
            Novel novel = novelRepository.findById(task.novelId)
                    .orElseThrow(() -> new RuntimeException("小说不存在"));

            List<Chapter> chapters = chapterRepository.findByNovelIdOrderByChapterNumberAsc(task.novelId);
            task.totalChapters = chapters.size();
            task.completedChapters = (int) chapters.stream()
                    .filter(c -> "GENERATED".equals(c.getStatus()) || "EDITED".equals(c.getStatus()))
                    .count();

            for (Chapter chapter : chapters) {
                // 检查是否被停止
                if (task.stopped) {
                    task.status = "STOPPED";
                    task.message = "用户停止了生成";
                    log.info("小说生成被用户停止: novelId={}", task.novelId);
                    return;
                }

                // 跳过已生成或已编辑的章节
                if ("GENERATED".equals(chapter.getStatus()) || "EDITED".equals(chapter.getStatus())) {
                    continue;
                }

                task.currentChapter = chapter.getChapterNumber();
                task.currentChapterTitle = chapter.getTitle();
                task.message = "正在生成第" + chapter.getChapterNumber() + "章: " + chapter.getTitle();
                log.info("后台生成章节: novelId={}, chapter={}/{}", task.novelId, chapter.getChapterNumber(), task.totalChapters);

                try {
                    // 生成章节内容
                    GenerateRequest req = new GenerateRequest();
                    req.setTargetWords(task.targetWords);
                    String prompt = buildChapterPrompt(novel, chapter, req);

                    chapter.setStatus("GENERATING");
                    chapterRepository.save(chapter);

                    String content = callLLM(prompt);
                    chapter.setContent(content);
                    chapter.setWordCount(content.length());
                    chapter.setStatus("GENERATED");

                    // 生成摘要
                    String summary = callLLM(String.format(SUMMARY_PROMPT, content));
                    chapter.setSummary(summary);
                    chapterRepository.save(chapter);

                    task.completedChapters++;
                    updateNovelWordCount(task.novelId);

                    log.info("章节生成完成: novelId={}, chapter={}, words={}",
                            task.novelId, chapter.getChapterNumber(), content.length());

                } catch (Exception e) {
                    log.error("章节生成失败: novelId={}, chapter={}", task.novelId, chapter.getChapterNumber(), e);
                    chapter.setStatus("OUTLINE"); // 恢复为大纲状态，以便下次重试
                    chapterRepository.save(chapter);
                    task.status = "ERROR";
                    task.currentChapter = chapter.getChapterNumber();
                    task.currentChapterTitle = chapter.getTitle();
                    task.message = "第" + chapter.getChapterNumber() + "章生成失败，已停止: " + e.getMessage();
                    return;
                }
            }

            task.status = "COMPLETED";
            task.message = "全部章节生成完成！";
            novel.setStatus("COMPLETED");
            novelRepository.save(novel);
            log.info("小说全部章节生成完成: novelId={}", task.novelId);

        } catch (Exception e) {
            log.error("小说生成任务异常: novelId={}", task.novelId, e);
            task.status = "ERROR";
            task.message = "生成出错: " + e.getMessage();
        } finally {
            // 延迟清理任务，让前端有时间获取最终状态
            CompletableFuture.delayedExecutor(60, TimeUnit.SECONDS).execute(() ->
                    activeTasks.remove(task.novelId));
        }
    }

    // ==================== 内部方法（复用 NovelServiceImpl 的 Prompt 逻辑） ====================

    private static final String CHAPTER_PROMPT = """
            你是一位资深网文作家，擅长创作%s风格的%s小说。
            请严格按照要求创作小说章节内容。
            
            【小说标题】%s
            【世界观设定】%s
            
            【角色设定】
            %s
            
            【前文摘要】
            %s
            
            【本章信息】
            第%d章：%s
            本章大纲：%s
            
            %s
            
            要求：
            1. 字数约 %d 字
            2. 情节紧凑，对话生动，符合角色性格
            3. 必须推进本章大纲中的剧情
            4. 直接输出正文内容，不要输出章节标题和任何标记
            """;

    private static final String SUMMARY_PROMPT = """
            请用200-300字概括以下小说章节的核心内容，包括主要事件、角色行为和情节推进：
            
            %s
            """;

    private String buildChapterPrompt(Novel novel, Chapter chapter, GenerateRequest request) {
        String charactersSummary = buildCharactersSummary(novel);
        String previousSummary = buildPreviousSummary(novel, chapter.getChapterNumber());
        int targetWords = (request != null && request.getTargetWords() != null) ? request.getTargetWords() : 2000;
        String userPrompt = (request != null && request.getUserPrompt() != null)
                ? "【用户附加要求】" + request.getUserPrompt() : "";

        return String.format(CHAPTER_PROMPT,
                novel.getStyle() != null ? novel.getStyle() : "不限",
                novel.getGenre(),
                novel.getTitle(),
                novel.getWorldSetting() != null ? novel.getWorldSetting() : "由你自由发挥",
                charactersSummary,
                previousSummary,
                chapter.getChapterNumber(),
                chapter.getTitle(),
                chapter.getOutline() != null ? chapter.getOutline() : "承接上文，推进剧情",
                userPrompt,
                targetWords);
    }

    private String buildCharactersSummary(Novel novel) {
        List<NovelCharacter> characters = characterRepository.findByNovelIdOrderByIdAsc(novel.getId());
        if (characters.isEmpty()) return "暂无角色设定";
        StringBuilder sb = new StringBuilder();
        for (NovelCharacter c : characters) {
            sb.append("- ").append(c.getName())
                    .append("（").append(roleTypeLabel(c.getRoleType())).append("）");
            if (c.getPersonality() != null) sb.append("，性格：").append(c.getPersonality());
            if (c.getBackground() != null) sb.append("，背景：").append(c.getBackground());
            sb.append("\n");
        }
        return sb.toString();
    }

    private String buildPreviousSummary(Novel novel, int currentChapterNumber) {
        List<Chapter> previousChapters = chapterRepository.findByNovelIdOrderByChapterNumberAsc(novel.getId());
        StringBuilder sb = new StringBuilder();
        int start = Math.max(0, currentChapterNumber - 4);
        for (int i = start; i < previousChapters.size(); i++) {
            Chapter ch = previousChapters.get(i);
            if (ch.getChapterNumber() >= currentChapterNumber) break;
            if (ch.getSummary() != null) {
                sb.append("第").append(ch.getChapterNumber()).append("章 ").append(ch.getTitle())
                        .append("：").append(ch.getSummary()).append("\n\n");
            }
        }
        return sb.isEmpty() ? "（这是第一章，无前文）" : sb.toString();
    }

    private String callLLM(String prompt) {
        return novelLlmClient.callOrThrow(prompt);
    }

    private void updateNovelWordCount(Long novelId) {
        List<Chapter> chapters = chapterRepository.findByNovelIdOrderByChapterNumberAsc(novelId);
        int totalWords = chapters.stream().mapToInt(ch -> ch.getWordCount() != null ? ch.getWordCount() : 0).sum();
        novelRepository.findById(novelId).ifPresent(novel -> {
            novel.setTotalWords(totalWords);
            novelRepository.save(novel);
        });
    }

    private String roleTypeLabel(String roleType) {
        return switch (roleType) {
            case "PROTAGONIST" -> "主角";
            case "ANTAGONIST" -> "反派";
            default -> "配角";
        };
    }

    private boolean isChapterDone(String status) {
        return "GENERATED".equals(status) || "EDITED".equals(status);
    }

    /** 内部任务信息 */
    private static class TaskInfo {
        volatile String status;
        volatile String message;
        volatile Integer currentChapter;
        volatile Integer totalChapters;
        volatile Integer completedChapters;
        volatile String currentChapterTitle;
        volatile boolean stopped;
        Long novelId;
        Long userId;
        int targetWords;
        int preChargedChapters;
        Future<?> future;
    }

    /** 大纲生成任务信息 */
    private static class OutlineTaskInfo {
        volatile String status;
        volatile String message;
        volatile boolean stopped;
        Long novelId;
        Long userId;
        int chapterCount;
        volatile Integer totalChapters;
        volatile Integer completedChapters;
        Future<?> future;
    }

    // ==================== 大纲生成逻辑 ====================

    private static final String OUTLINE_PROMPT = """
            你是一位资深网文作家，擅长创作%s类型的小说。

            请为以下小说生成详细的章节大纲：

            【小说标题】%s
            【小说类型】%s
            【写作风格】%s
            【小说简介】%s
            【世界观设定】%s
            【角色设定】
            %s

            请生成 %d 个章节的大纲，每章包含标题和200字左右的内容概要。

            严格按以下 JSON 数组格式输出，不要输出其他内容：
            [{"title": "第一章 章节标题", "outline": "本章内容概要..."}, ...]
            """;

    private void runOutlineGeneration(OutlineTaskInfo task) {
        try {
            Novel novel = novelRepository.findById(task.novelId)
                    .orElseThrow(() -> new RuntimeException("小说不存在"));

            String charactersSummary = buildCharactersSummary(novel);
            String prompt = String.format(OUTLINE_PROMPT,
                    novel.getGenre(),
                    novel.getTitle(),
                    novel.getGenre(),
                    novel.getStyle() != null ? novel.getStyle() : "不限",
                    novel.getDescription() != null ? novel.getDescription() : "无",
                    novel.getWorldSetting() != null ? novel.getWorldSetting() : "无",
                    charactersSummary,
                    task.chapterCount);

            log.info("开始生成大纲: novelId={}, chapterCount={}", task.novelId, task.chapterCount);

            task.totalChapters = task.chapterCount;
            task.completedChapters = 0;

            String result = callLLM(prompt);

            if (task.stopped) {
                task.status = "STOPPED";
                task.message = "用户停止了生成";
                return;
            }

            List<OutlineItem> outlineItems = parseOutlineJson(result);
            if (outlineItems == null || outlineItems.isEmpty()) {
                throw new RuntimeException("Outline JSON parse failed");
            }
            if (outlineItems.size() != task.chapterCount) {
                throw new RuntimeException("Outline count mismatch: expected=" + task.chapterCount + ", actual=" + outlineItems.size());
            }

            // 追加生成大纲章节：从当前最大章节号之后开始追加 OUTLINE 章节
            List<Chapter> existingChapters = chapterRepository.findByNovelIdOrderByChapterNumberAsc(task.novelId);
            // Do not delete existing OUTLINE chapters here; we append new outline chapters after the current max.
            // If users want to replace outline chapters, call DELETE /novels/{id}/outline-chapters first.

            // 获取已有内容章节的最大章节号
            int startChapterNumber = existingChapters.stream()
                    .mapToInt(Chapter::getChapterNumber)
                    .max()
                    .orElse(0) + 1;

            List<Chapter> chapters = new java.util.ArrayList<>();
            for (int i = 0; i < outlineItems.size(); i++) {
                Chapter chapter = new Chapter();
                chapter.setNovel(novel);
                chapter.setChapterNumber(startChapterNumber + i);
                chapter.setTitle(sanitizeOutlineTitle(outlineItems.get(i).title));
                chapter.setOutline(outlineItems.get(i).outline);
                chapter.setStatus("OUTLINE");
                chapters.add(chapter);
            }
            chapterRepository.saveAll(chapters);

            // Charge AFTER outline chapters are successfully saved. If charging fails, rollback the inserted chapters.
            try {
                billingService.consumePoints(
                        task.userId,
                        BillingCosts.OUTLINE * chapters.size(),
                        "OUTLINE_BATCH",
                        task.novelId,
                        "Generate outline (" + chapters.size() + " chapters) for novelId=" + task.novelId);
            } catch (Exception billingEx) {
                chapterRepository.deleteAll(chapters);
                throw billingEx;
            }

            task.completedChapters = chapters.size();

            novel.setStatus("IN_PROGRESS");
            novelRepository.save(novel);

            task.status = "COMPLETED";
            task.message = "大纲生成完成";
            log.info("大纲生成完成: novelId={}, chapters={}", task.novelId, chapters.size());

        } catch (Exception e) {
            log.error("大纲生成失败: novelId={}", task.novelId, e);
            task.status = "ERROR";
            task.message = "生成失败: " + e.getMessage();
        } finally {
            CompletableFuture.delayedExecutor(60, TimeUnit.SECONDS).execute(() ->
                    activeOutlineTasks.remove(task.novelId));
        }
    }

    private List<OutlineItem> parseOutlineJson(String result) {
        try {
            int start = result.indexOf('[');
            int end = result.lastIndexOf(']');
            if (start >= 0 && end > start) {
                String json = result.substring(start, end + 1);
                return new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(json, new com.fasterxml.jackson.core.type.TypeReference<>() {});
            }
        } catch (Exception e) {
            log.warn("解析大纲 JSON 失败: {}", e.getMessage());
        }
        return java.util.List.of();
    }

    private record OutlineItem(String title, String outline) {}

    private String sanitizeOutlineTitle(String title) {
        if (title == null) return "";
        String t = title.trim();
        // Strip common chapter-number prefixes like "第1章", "第一章", "第十章:" etc.
        t = t.replaceAll("^\\s*第\\s*([0-9]+|[一二三四五六七八九十百千万零两]+)\\s*章\\s*[:：\\-—.、\\s]*", "");
        return t.isBlank() ? title.trim() : t;
    }
}
