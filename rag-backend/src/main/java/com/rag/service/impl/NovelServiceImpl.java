package com.rag.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rag.dto.BranchSuggestionResponse;
import com.rag.dto.ChapterResponse;
import com.rag.dto.CharacterRequest;
import com.rag.dto.GenerateRequest;
import com.rag.dto.NovelRequest;
import com.rag.dto.NovelResponse;
import com.rag.dto.NovelStatistics;
import com.rag.dto.WordFrequency;
import com.rag.entity.Chapter;
import com.rag.entity.Novel;
import com.rag.entity.NovelCharacter;
import com.rag.repository.ChapterRepository;
import com.rag.repository.NovelCharacterRepository;
import com.rag.repository.NovelRepository;
import com.rag.security.SecurityUtils;
import com.rag.service.BillingCosts;
import com.rag.service.BillingService;
import com.rag.service.NovelService;
import com.rag.service.SensitiveWordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NovelServiceImpl implements NovelService {

    private static final String LLM_FALLBACK_MESSAGE = "AI 生成服务暂时不可用，请稍后重试。";

    private static final String OUTLINE_PROMPT = """
            你是一位擅长创作网络小说的资深编辑，请根据以下信息生成章节大纲。

            【小说标题】%s
            【小说类型】%s
            【写作风格】%s
            【小说简介】%s
            【世界观设定】%s
            【角色设定】
            %s

            请输出 %d 个章节的大纲。每章都需要包含：
            1. 一个清晰的章节标题
            2. 一段 80-160 字的剧情概要

            只输出 JSON 数组，不要输出其他说明文字。
            输出格式：
            [{"title":"第一章 标题","outline":"本章剧情概要"}]
            """;

    private static final String CHAPTER_PROMPT = """
            你是一位擅长创作%s风格%s小说的作者，请根据以下设定写出当前章节正文。

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

            写作要求：
            1. 字数控制在约 %d 字
            2. 情节紧凑、人物行为符合设定
            3. 必须围绕本章大纲推进
            4. 直接输出正文，不要输出标题或额外说明
            """;

    private static final String SUMMARY_PROMPT = """
            请用 200-300 字总结下面这一章的核心内容，突出主要事件、关键人物行动和情节推进。

            %s
            """;

    private static final String BRANCH_SUGGESTION_PROMPT = """
            你是一名擅长设计网络小说剧情走向的资深编辑，请基于下面的信息，为当前章节提供 3 个后续剧情分支建议。

            【小说标题】%s
            【题材类型】%s
            【写作风格】%s
            【小说简介】%s
            【世界观设定】%s

            【角色设定】
            %s

            【前文摘要】
            %s

            【当前章节】
            第%s章 %s
            【本章大纲】%s
            【本章摘要】%s
            【本章正文节选】%s

            请给出 3 个明显不同的延续方向，每个方向都要兼顾可写性与追更吸引力。
            只输出 JSON 数组，不要输出任何解释文字。
            每个对象必须包含 title、direction、conflict、hook 四个字段。
            示例：
            [{"title":"旧敌提前现身","direction":"主角刚稳住局面，旧敌突然现身打乱计划。","conflict":"主角必须在救人和隐藏身份之间做选择。","hook":"最后揭出旧敌手里握着主角最在意之人的线索。"}]
            """;

    private final NovelRepository novelRepository;
    private final ChapterRepository chapterRepository;
    private final NovelCharacterRepository characterRepository;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final SecurityUtils securityUtils;
    private final BillingService billingService;
    private final SensitiveWordService sensitiveWordService;

    @Value("${rag.llm.timeout-seconds:45}")
    private long llmTimeoutSeconds;

    @Override
    @Transactional
    public NovelResponse createNovel(NovelRequest request) {
        Long userId = securityUtils.getCurrentUserId();

        Novel novel = new Novel();
        novel.setUserId(userId);
        novel.setTitle(request.getTitle());
        novel.setGenre(request.getGenre());
        novel.setStyle(request.getStyle());
        novel.setDescription(request.getDescription());
        novel.setWorldSetting(request.getWorldSetting());
        novel.setCoverUrl(request.getCoverUrl());
        novel.setStatus("DRAFT");

        novelRepository.save(novel);
        log.info("Created novel: id={}, title={}", novel.getId(), novel.getTitle());
        return toNovelResponse(novel);
    }

    @Override
    public List<NovelResponse> listMyNovels() {
        Long userId = securityUtils.getCurrentUserId();
        return novelRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(this::toNovelResponse)
                .collect(Collectors.toList());
    }

    @Override
    public NovelResponse getNovel(Long novelId) {
        Novel novel = getNovelWithPermission(novelId);
        NovelResponse response = toNovelResponse(novel);
        response.setChapters(novel.getChapters().stream().map(this::toChapterBrief).collect(Collectors.toList()));
        response.setCharacters(novel.getCharacters().stream().map(this::toCharacterInfo).collect(Collectors.toList()));
        return response;
    }

    @Override
    @Transactional
    public NovelResponse updateNovel(Long novelId, NovelRequest request) {
        Novel novel = getNovelWithPermission(novelId);
        novel.setTitle(request.getTitle());
        novel.setGenre(request.getGenre());
        novel.setStyle(request.getStyle());
        novel.setDescription(request.getDescription());
        novel.setWorldSetting(request.getWorldSetting());
        novel.setCoverUrl(request.getCoverUrl());
        novelRepository.save(novel);
        return toNovelResponse(novel);
    }

    @Override
    @Transactional
    public void deleteNovel(Long novelId) {
        Novel novel = getNovelWithPermission(novelId);
        novelRepository.delete(novel);
        log.info("Deleted novel: id={}", novelId);
    }

    @Override
    @Transactional
    public NovelResponse.CharacterInfo addCharacter(Long novelId, CharacterRequest request) {
        Novel novel = getNovelWithPermission(novelId);

        NovelCharacter character = new NovelCharacter();
        character.setNovel(novel);
        character.setName(request.getName());
        character.setRoleType(hasText(request.getRoleType()) ? request.getRoleType() : "SUPPORTING");
        character.setPersonality(request.getPersonality());
        character.setBackground(request.getBackground());
        character.setAppearance(request.getAppearance());
        character.setRelationships(request.getRelationships());
        characterRepository.save(character);

        return toCharacterInfo(character);
    }

    @Override
    @Transactional
    public NovelResponse.CharacterInfo updateCharacter(Long novelId, Long characterId, CharacterRequest request) {
        NovelCharacter character = getCharacterWithPermission(novelId, characterId);

        character.setName(request.getName());
        character.setRoleType(request.getRoleType());
        character.setPersonality(request.getPersonality());
        character.setBackground(request.getBackground());
        character.setAppearance(request.getAppearance());
        character.setRelationships(request.getRelationships());
        characterRepository.save(character);

        return toCharacterInfo(character);
    }

    @Override
    @Transactional
    public NovelResponse.CharacterInfo updateCharacterWithReplace(Long novelId, Long characterId, String oldName, CharacterRequest request) {
        NovelCharacter character = getCharacterWithPermission(novelId, characterId);
        String newName = request.getName();

        if (hasText(oldName) && hasText(newName) && !oldName.equals(newName)) {
            String oldShortName = oldName.length() >= 2 ? oldName.substring(oldName.length() - 2) : oldName;
            String newShortName = newName.length() >= 2 ? newName.substring(newName.length() - 2) : newName;

            List<Chapter> chapters = chapterRepository.findByNovelIdOrderByChapterNumberAsc(novelId);
            for (Chapter chapter : chapters) {
                boolean updated = false;

                if (chapter.getTitle() != null) {
                    String title = replaceNames(chapter.getTitle(), oldName, newName, oldShortName, newShortName);
                    if (!title.equals(chapter.getTitle())) {
                        chapter.setTitle(title);
                        updated = true;
                    }
                }

                if (chapter.getOutline() != null) {
                    String outline = replaceNames(chapter.getOutline(), oldName, newName, oldShortName, newShortName);
                    if (!outline.equals(chapter.getOutline())) {
                        chapter.setOutline(outline);
                        updated = true;
                    }
                }

                if (chapter.getContent() != null) {
                    String content = replaceNames(chapter.getContent(), oldName, newName, oldShortName, newShortName);
                    if (!content.equals(chapter.getContent())) {
                        chapter.setContent(content);
                        updated = true;
                    }
                }

                if (updated) {
                    chapterRepository.save(chapter);
                }
            }
        }

        character.setName(newName);
        character.setRoleType(request.getRoleType());
        character.setPersonality(request.getPersonality());
        character.setBackground(request.getBackground());
        character.setAppearance(request.getAppearance());
        character.setRelationships(request.getRelationships());
        characterRepository.save(character);

        return toCharacterInfo(character);
    }

    @Override
    @Transactional
    public void deleteCharacter(Long novelId, Long characterId) {
        NovelCharacter character = getCharacterWithPermission(novelId, characterId);
        characterRepository.delete(character);
    }

    @Override
    @Transactional
    public void deleteChapter(Long novelId, Long chapterId) {
        Chapter chapter = getChapterWithPermission(novelId, chapterId);
        chapterRepository.delete(chapter);
    }

    @Override
    @Transactional
    public void deleteOutlineChapters(Long novelId) {
        getNovelWithPermission(novelId);
        chapterRepository.deleteByNovelIdAndStatus(novelId, "OUTLINE");
    }

    @Override
    @Transactional
    public List<NovelResponse.ChapterBrief> generateOutline(Long novelId, Integer chapterCount) {
        Novel novel = getNovelWithPermission(novelId);
        billingService.consumePoints(
                novel.getUserId(),
                BillingCosts.OUTLINE,
                "OUTLINE",
                novelId,
                "Generate outline for novel: " + novel.getTitle());
        int count = chapterCount != null ? chapterCount : 10;

        String prompt = String.format(
                OUTLINE_PROMPT,
                novel.getTitle(),
                defaultText(novel.getGenre(), "未设定"),
                defaultText(novel.getStyle(), "不限"),
                defaultText(novel.getDescription(), "暂无简介"),
                defaultText(novel.getWorldSetting(), "暂无世界观设定"),
                buildCharactersSummary(novel),
                count
        );

        String result = callLLM(prompt);
        List<OutlineItem> outlineItems = parseOutlineJson(result);

        List<Chapter> existingChapters = chapterRepository.findByNovelIdOrderByChapterNumberAsc(novelId);
        List<Chapter> toDelete = existingChapters.stream()
                .filter(chapter -> "OUTLINE".equals(chapter.getStatus()))
                .collect(Collectors.toList());
        chapterRepository.deleteAll(toDelete);

        int startChapterNumber = existingChapters.stream()
                .filter(chapter -> !"OUTLINE".equals(chapter.getStatus()))
                .mapToInt(Chapter::getChapterNumber)
                .max()
                .orElse(0) + 1;

        List<Chapter> chapters = new ArrayList<>();
        for (int i = 0; i < outlineItems.size(); i++) {
            OutlineItem item = outlineItems.get(i);
            Chapter chapter = new Chapter();
            chapter.setNovel(novel);
            chapter.setChapterNumber(startChapterNumber + i);
            chapter.setTitle(item.title());
            chapter.setOutline(item.outline());
            chapter.setStatus("OUTLINE");
            chapters.add(chapter);
        }

        chapterRepository.saveAll(chapters);
        novel.setStatus("IN_PROGRESS");
        novelRepository.save(novel);

        return chapters.stream().map(this::toChapterBrief).collect(Collectors.toList());
    }

    @Override
    public ChapterResponse getChapter(Long novelId, Long chapterId) {
        Chapter chapter = getChapterWithPermission(novelId, chapterId);
        return toChapterResponse(chapter);
    }

    @Override
    @Transactional
    public ChapterResponse updateChapter(Long novelId, Long chapterId, String title, String outline, String content) {
        Chapter chapter = getChapterWithPermission(novelId, chapterId);

        if (title != null) {
            chapter.setTitle(title);
        }
        if (outline != null) {
            chapter.setOutline(outline);
        }
        if (content != null) {
            chapter.setContent(content);
            chapter.setWordCount(content.length());
            chapter.setStatus("EDITED");
            if (chapter.getCompletedAt() == null) {
                chapter.setCompletedAt(LocalDateTime.now());
            }
        }

        chapterRepository.save(chapter);
        updateNovelWordCount(novelId);
        return toChapterResponse(chapter);
    }

    @Override
    @Transactional
    public ChapterResponse updateChapterNotes(Long novelId, Long chapterId, String notes) {
        Chapter chapter = getChapterWithPermission(novelId, chapterId);
        chapter.setNotes(notes);
        chapterRepository.save(chapter);
        return toChapterResponse(chapter);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchSuggestionResponse> suggestChapterBranches(Long novelId, Long chapterId) {
        Novel novel = getNovelWithPermission(novelId);
        Chapter chapter = getChapterWithPermission(novelId, chapterId);
        try {
            String result = callLLM(buildBranchSuggestionPrompt(novel, chapter));
            List<BranchSuggestionResponse> suggestions = parseBranchSuggestions(result);
            return ensureThreeSuggestions(suggestions, novel, chapter);
        } catch (Exception ex) {
            log.warn("Failed to generate branch suggestions, falling back: novelId={}, chapterId={}, error={}",
                    novelId, chapterId, ex.getMessage());
            return buildFallbackBranchSuggestions(novel, chapter);
        }
    }

    @Override
    @Transactional
    public ChapterResponse generateChapter(Long novelId, Long chapterId, GenerateRequest request) {
        Novel novel = getNovelWithPermission(novelId);
        Chapter chapter = getChapterWithPermission(novelId, chapterId);

        billingService.consumePoints(
                novel.getUserId(),
                BillingCosts.CHAPTER,
                "CHAPTER",
                chapterId,
                "Generate chapter " + chapter.getChapterNumber() + " for novel: " + novel.getTitle());

        chapter.setStatus("GENERATING");
        chapterRepository.save(chapter);

        String content = callLLM(buildChapterPrompt(novel, chapter, request));
        chapter.setContent(content);
        chapter.setWordCount(content.length());
        chapter.setStatus("GENERATED");
        chapter.setCompletedAt(LocalDateTime.now());

        List<String> sensitiveWords = sensitiveWordService.detectSensitiveWords(content);
        if (!sensitiveWords.isEmpty()) {
            log.warn("Sensitive words found in chapter generation: novelId={}, chapter={}, words={}",
                    novelId, chapter.getChapterNumber(), sensitiveWords);
        }

        chapter.setSummary(callLLM(String.format(SUMMARY_PROMPT, content)));
        chapterRepository.save(chapter);
        updateNovelWordCount(novelId);
        return toChapterResponse(chapter);
    }

    @Override
    public Flux<String> generateChapterStream(Long novelId, Long chapterId, GenerateRequest request) {
        Novel novel = getNovelWithPermission(novelId);
        Chapter chapter = getChapterWithPermission(novelId, chapterId);

        billingService.consumePoints(
                novel.getUserId(),
                BillingCosts.CHAPTER,
                "CHAPTER_STREAM",
                chapterId,
                "Generate chapter stream " + chapter.getChapterNumber() + " for novel: " + novel.getTitle());

        chapter.setStatus("GENERATING");
        chapterRepository.save(chapter);

        String prompt = buildChapterPrompt(novel, chapter, request);
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
        StringBuilder fullContent = new StringBuilder();

        CompletableFuture.runAsync(() -> {
            try {
                chatClient.prompt().user(prompt).stream().content()
                        .timeout(Duration.ofSeconds(Math.max(10, llmTimeoutSeconds)))
                        .onErrorResume(java.util.concurrent.TimeoutException.class, e -> Flux.empty())
                        .doOnNext(chunk -> {
                            fullContent.append(chunk);
                            sink.tryEmitNext(chunk);
                        })
                        .doOnComplete(() -> {
                            try {
                                String content = fullContent.toString();
                                chapter.setContent(content);
                                chapter.setWordCount(content.length());
                                chapter.setStatus("GENERATED");
                                chapter.setCompletedAt(LocalDateTime.now());
                                chapter.setSummary(callLLM(String.format(SUMMARY_PROMPT, content)));
                                chapterRepository.save(chapter);
                                updateNovelWordCount(novelId);
                            } catch (Exception e) {
                                log.error("Failed to finalize stream chapter generation", e);
                            }
                            sink.tryEmitComplete();
                        })
                        .doOnError(error -> {
                            log.error("Failed to stream chapter generation", error);
                            sink.tryEmitError(error);
                        })
                        .subscribe();
            } catch (Exception e) {
                log.error("Failed to start stream chapter generation", e);
                sink.tryEmitError(e);
            }
        });

        return sink.asFlux();
    }

    private Novel getNovelWithPermission(Long novelId) {
        Long userId = securityUtils.getCurrentUserId();
        return novelRepository.findByIdAndUserId(novelId, userId)
                .orElseThrow(() -> new RuntimeException("小说不存在或无权访问"));
    }

    private Chapter getChapterWithPermission(Long novelId, Long chapterId) {
        getNovelWithPermission(novelId);
        return chapterRepository.findByIdAndNovelId(chapterId, novelId)
                .orElseThrow(() -> new RuntimeException("章节不存在或不属于当前小说"));
    }

    private NovelCharacter getCharacterWithPermission(Long novelId, Long characterId) {
        getNovelWithPermission(novelId);
        return characterRepository.findByIdAndNovelId(characterId, novelId)
                .orElseThrow(() -> new RuntimeException("角色不存在或不属于当前小说"));
    }

    private String buildCharactersSummary(Novel novel) {
        List<NovelCharacter> characters = characterRepository.findByNovelIdOrderByIdAsc(novel.getId());
        if (characters.isEmpty()) {
            return "暂无角色设定";
        }

        StringBuilder sb = new StringBuilder();
        for (NovelCharacter character : characters) {
            sb.append("- ").append(character.getName())
                    .append("（").append(roleTypeLabel(character.getRoleType())).append("）");
            if (hasText(character.getPersonality())) {
                sb.append("，性格：").append(character.getPersonality());
            }
            if (hasText(character.getBackground())) {
                sb.append("，背景：").append(character.getBackground());
            }
            if (hasText(character.getRelationships())) {
                sb.append("，关系：").append(character.getRelationships());
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private String buildPreviousSummary(Novel novel, int currentChapterNumber) {
        List<Chapter> chapters = chapterRepository.findByNovelIdOrderByChapterNumberAsc(novel.getId());
        StringBuilder sb = new StringBuilder();
        int minChapterNumber = Math.max(1, currentChapterNumber - 3);

        for (Chapter chapter : chapters) {
            Integer chapterNumber = chapter.getChapterNumber();
            if (chapterNumber == null || chapterNumber < minChapterNumber || chapterNumber >= currentChapterNumber) {
                continue;
            }
            if (hasText(chapter.getSummary())) {
                sb.append("第").append(chapterNumber).append("章 ")
                        .append(defaultText(chapter.getTitle(), "未命名章节"))
                        .append("：").append(chapter.getSummary())
                        .append("\n\n");
            }
        }

        return sb.isEmpty() ? "这是第一章，或前文还没有可用摘要。" : sb.toString();
    }

    private String buildChapterPrompt(Novel novel, Chapter chapter, GenerateRequest request) {
        int targetWords = request != null && request.getTargetWords() != null ? request.getTargetWords() : 2000;
        String userPrompt = request != null && hasText(request.getUserPrompt())
                ? "【额外要求】\n" + request.getUserPrompt()
                : "";

        return String.format(
                CHAPTER_PROMPT,
                defaultText(novel.getStyle(), "不限"),
                defaultText(novel.getGenre(), "未设定"),
                novel.getTitle(),
                defaultText(novel.getWorldSetting(), "由你自由发挥"),
                buildCharactersSummary(novel),
                buildPreviousSummary(novel, chapter.getChapterNumber()),
                chapter.getChapterNumber(),
                defaultText(chapter.getTitle(), "未命名章节"),
                defaultText(chapter.getOutline(), "承接上文，推进主要矛盾。"),
                userPrompt,
                targetWords
        );
    }

    private String buildBranchSuggestionPrompt(Novel novel, Chapter chapter) {
        String content = hasText(chapter.getContent())
                ? truncateText(chapter.getContent(), 1200)
                : "当前章节正文尚未完成，可根据已有设定设计后续走向。";

        return String.format(
                BRANCH_SUGGESTION_PROMPT,
                novel.getTitle(),
                defaultText(novel.getGenre(), "未设定"),
                defaultText(novel.getStyle(), "未设定"),
                defaultText(novel.getDescription(), "暂无简介"),
                defaultText(novel.getWorldSetting(), "暂无世界观设定"),
                buildCharactersSummary(novel),
                buildPreviousSummary(novel, chapter.getChapterNumber()),
                chapter.getChapterNumber(),
                defaultText(chapter.getTitle(), "未命名章节"),
                defaultText(chapter.getOutline(), "暂无章节大纲，可结合上下文自由补足。"),
                defaultText(chapter.getSummary(), "暂无章节摘要，请重点参考大纲与正文。"),
                content
        );
    }

    private String callLLM(String prompt) {
        try {
            List<String> chunks = chatClient.prompt().user(prompt)
                    .stream().content()
                    .timeout(Duration.ofSeconds(Math.max(10, llmTimeoutSeconds)))
                    .onErrorResume(java.util.concurrent.TimeoutException.class, e -> Flux.empty())
                    .collectList()
                    .block(Duration.ofMinutes(10));

            if (chunks == null || chunks.isEmpty()) {
                return LLM_FALLBACK_MESSAGE;
            }

            String result = String.join("", chunks);
            return result.isBlank() ? LLM_FALLBACK_MESSAGE : result;
        } catch (Exception ex) {
            log.warn("LLM call failed: {}", ex.getMessage());
            return LLM_FALLBACK_MESSAGE;
        }
    }

    private List<OutlineItem> parseOutlineJson(String result) {
        try {
            int start = result.indexOf('[');
            int end = result.lastIndexOf(']');
            if (start >= 0 && end > start) {
                String json = result.substring(start, end + 1);
                return objectMapper.readValue(json, new TypeReference<List<OutlineItem>>() {});
            }
        } catch (Exception e) {
            log.warn("Failed to parse outline JSON: {}", e.getMessage());
        }
        return List.of();
    }

    private List<BranchSuggestionResponse> parseBranchSuggestions(String result) {
        try {
            int start = result.indexOf('[');
            int end = result.lastIndexOf(']');
            if (start >= 0 && end > start) {
                String json = result.substring(start, end + 1);
                List<BranchSuggestionResponse> suggestions = objectMapper.readValue(
                        json, new TypeReference<List<BranchSuggestionResponse>>() {});
                return suggestions.stream()
                        .filter(item -> item != null && hasText(item.getTitle()) && hasText(item.getDirection()))
                        .limit(3)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("Failed to parse branch suggestions: {}", e.getMessage());
        }
        return List.of();
    }

    private List<BranchSuggestionResponse> ensureThreeSuggestions(
            List<BranchSuggestionResponse> suggestions, Novel novel, Chapter chapter) {
        List<BranchSuggestionResponse> safeSuggestions = new ArrayList<>();
        if (suggestions != null) {
            safeSuggestions.addAll(suggestions.stream()
                    .filter(item -> item != null && hasText(item.getTitle()) && hasText(item.getDirection()))
                    .limit(3)
                    .collect(Collectors.toList()));
        }

        if (safeSuggestions.size() >= 3) {
            return safeSuggestions;
        }

        List<BranchSuggestionResponse> fallbackSuggestions = buildFallbackBranchSuggestions(novel, chapter);
        for (BranchSuggestionResponse fallback : fallbackSuggestions) {
            if (safeSuggestions.size() >= 3) {
                break;
            }
            safeSuggestions.add(fallback);
        }
        return safeSuggestions;
    }

    private List<BranchSuggestionResponse> buildFallbackBranchSuggestions(Novel novel, Chapter chapter) {
        String title = defaultText(chapter.getTitle(), "当前章节");
        String genre = defaultText(novel.getGenre(), "当前题材");
        String summary = defaultText(chapter.getSummary(), defaultText(chapter.getOutline(), "当前剧情刚刚推进到关键节点。"));

        List<BranchSuggestionResponse> suggestions = new ArrayList<>();
        suggestions.add(createSuggestion(
                "冲突升级",
                "围绕" + title + "继续推进，让主角遭遇更强的阻力。",
                "原本可控的局面突然失衡，主角必须立刻做出选择。",
                "在章节结尾抛出一个更大的危机，带动下一章继续阅读。"
        ));
        suggestions.add(createSuggestion(
                "秘密揭开",
                "从" + summary,
                "把现有线索串起来，揭开一个和主角或核心势力有关的秘密。",
                "秘密只揭开一半，留下真正关键的答案到下一章。"
        ));
        suggestions.add(createSuggestion(
                "人物变局",
                "结合" + genre + "风格，让重要人物关系发生新的变化。",
                "盟友、亲人或对手的立场突然变化，打乱主角原计划。",
                "用一场关系反转或意外站队，制造新的情绪张力。"
        ));
        return suggestions;
    }

    private BranchSuggestionResponse createSuggestion(String title, String direction, String conflict, String hook) {
        BranchSuggestionResponse suggestion = new BranchSuggestionResponse();
        suggestion.setTitle(title);
        suggestion.setDirection(direction);
        suggestion.setConflict(conflict);
        suggestion.setHook(hook);
        return suggestion;
    }

    private void updateNovelWordCount(Long novelId) {
        List<Chapter> chapters = chapterRepository.findByNovelIdOrderByChapterNumberAsc(novelId);
        int totalWords = chapters.stream()
                .mapToInt(chapter -> chapter.getWordCount() != null ? chapter.getWordCount() : 0)
                .sum();
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

    private NovelResponse toNovelResponse(Novel novel) {
        int computedWords = 0;
        if (novel.getChapters() != null) {
            computedWords = novel.getChapters().stream()
                    .mapToInt(chapter -> {
                        if (chapter.getWordCount() != null && chapter.getWordCount() > 0) {
                            return chapter.getWordCount();
                        }
                        return chapter.getContent() != null ? chapter.getContent().length() : 0;
                    })
                    .sum();
        }

        int effectiveWords = (novel.getTotalWords() != null && novel.getTotalWords() > 0)
                ? novel.getTotalWords()
                : computedWords;

        return NovelResponse.builder()
                .id(novel.getId())
                .title(novel.getTitle())
                .genre(novel.getGenre())
                .style(novel.getStyle())
                .description(novel.getDescription())
                .worldSetting(novel.getWorldSetting())
                .status(novel.getStatus())
                .totalWords(effectiveWords)
                .chapterCount(novel.getChapters() != null ? novel.getChapters().size() : 0)
                .coverUrl(novel.getCoverUrl())
                .createdAt(novel.getCreatedAt())
                .updatedAt(novel.getUpdatedAt())
                .build();
    }

    private NovelResponse.ChapterBrief toChapterBrief(Chapter chapter) {
        return NovelResponse.ChapterBrief.builder()
                .id(chapter.getId())
                .chapterNumber(chapter.getChapterNumber())
                .title(chapter.getTitle())
                .outline(chapter.getOutline())
                .wordCount(chapter.getWordCount())
                .status(chapter.getStatus())
                .completedAt(chapter.getCompletedAt())
                .build();
    }

    private NovelResponse.CharacterInfo toCharacterInfo(NovelCharacter character) {
        return NovelResponse.CharacterInfo.builder()
                .id(character.getId())
                .name(character.getName())
                .roleType(character.getRoleType())
                .personality(character.getPersonality())
                .background(character.getBackground())
                .appearance(character.getAppearance())
                .relationships(character.getRelationships())
                .build();
    }

    private ChapterResponse toChapterResponse(Chapter chapter) {
        return ChapterResponse.builder()
                .id(chapter.getId())
                .novelId(chapter.getNovel().getId())
                .chapterNumber(chapter.getChapterNumber())
                .title(chapter.getTitle())
                .outline(chapter.getOutline())
                .content(chapter.getContent())
                .summary(chapter.getSummary())
                .wordCount(chapter.getWordCount())
                .status(chapter.getStatus())
                .createdAt(chapter.getCreatedAt())
                .updatedAt(chapter.getUpdatedAt())
                .completedAt(chapter.getCompletedAt())
                .notes(chapter.getNotes())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public String exportNovelAsText(Long novelId) {
        Novel novel = getNovelWithPermission(novelId);
        List<Chapter> chapters = chapterRepository.findByNovelIdOrderByChapterNumberAsc(novelId);

        StringBuilder sb = new StringBuilder();
        sb.append(novel.getTitle()).append("\n");
        sb.append("=".repeat(Math.max(1, novel.getTitle().length() * 2))).append("\n\n");

        if (hasText(novel.getDescription())) {
            sb.append("简介：").append(novel.getDescription()).append("\n\n");
        }

        for (Chapter chapter : chapters) {
            if (!hasText(chapter.getContent())) {
                continue;
            }
            sb.append("第").append(chapter.getChapterNumber()).append("章 ")
                    .append(defaultText(chapter.getTitle(), "未命名章节"))
                    .append("\n\n");
            sb.append(chapter.getContent()).append("\n\n\n");
        }

        return sb.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public String exportNovelAsMarkdown(Long novelId) {
        Novel novel = getNovelWithPermission(novelId);
        List<Chapter> chapters = chapterRepository.findByNovelIdOrderByChapterNumberAsc(novelId);

        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(novel.getTitle()).append("\n\n");

        if (hasText(novel.getDescription())) {
            sb.append("> ").append(novel.getDescription().replace("\n", "\n> ")).append("\n\n");
        }

        sb.append("---\n\n");

        for (Chapter chapter : chapters) {
            if (!hasText(chapter.getContent())) {
                continue;
            }
            sb.append("## 第").append(chapter.getChapterNumber()).append("章 ")
                    .append(defaultText(chapter.getTitle(), "未命名章节"))
                    .append("\n\n");
            sb.append(chapter.getContent()).append("\n\n");
        }

        return sb.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public NovelStatistics getStatistics(Long novelId) {
        getNovelWithPermission(novelId);
        List<Chapter> chapters = chapterRepository.findByNovelIdOrderByChapterNumberAsc(novelId);

        NovelStatistics stats = new NovelStatistics();
        stats.setTotalChapters(chapters.size());

        int completedCount = 0;
        int totalWords = 0;
        int maxWords = 0;
        int minWords = Integer.MAX_VALUE;

        for (Chapter chapter : chapters) {
            if (!hasText(chapter.getContent())) {
                continue;
            }
            completedCount++;
            int words = chapter.getWordCount() != null ? chapter.getWordCount() : 0;
            totalWords += words;
            maxWords = Math.max(maxWords, words);
            minWords = Math.min(minWords, words);
        }

        stats.setCompletedChapters(completedCount);
        stats.setTotalWords(totalWords);
        stats.setAverageWordsPerChapter(completedCount > 0 ? totalWords / completedCount : 0);
        stats.setLongestChapterWords(maxWords);
        stats.setShortestChapterWords(minWords == Integer.MAX_VALUE ? 0 : minWords);
        stats.setCompletionRate(chapters.isEmpty() ? 0.0 : (double) completedCount / chapters.size() * 100);
        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WordFrequency> getWordFrequency(Long novelId) {
        getNovelWithPermission(novelId);
        List<Chapter> chapters = chapterRepository.findByNovelIdOrderByChapterNumberAsc(novelId);

        Set<String> stopWords = Set.of(
                "我们", "你们", "他们", "自己", "已经", "然后", "因为", "所以", "如果",
                "一个", "这个", "那个", "不是", "没有", "什么", "怎么", "时候", "突然",
                "只是", "还是", "可以", "不能", "不会", "一下", "一种", "一样",
                "看着", "说道", "起来", "进去", "出来", "这里", "那里", "现在", "终于"
        );

        Map<String, Integer> wordCount = new HashMap<>();

        for (Chapter chapter : chapters) {
            String content = chapter.getContent();
            if (!hasText(content)) {
                continue;
            }

            for (int len = 4; len >= 2; len--) {
                for (int i = 0; i <= content.length() - len; i++) {
                    String word = content.substring(i, i + len);
                    if (word.matches("[\\u4e00-\\u9fa5]{" + len + "}") && !stopWords.contains(word)) {
                        wordCount.merge(word, 1, Integer::sum);
                    }
                }
            }
        }

        List<Map.Entry<String, Integer>> sortedWords = wordCount.entrySet().stream()
                .filter(entry -> entry.getValue() >= 5)
                .sorted((left, right) -> right.getValue().compareTo(left.getValue()))
                .collect(Collectors.toList());

        Set<String> filtered = new HashSet<>();
        for (Map.Entry<String, Integer> entry : sortedWords) {
            String word = entry.getKey();
            boolean isSubstring = false;
            for (Map.Entry<String, Integer> other : sortedWords) {
                if (other.getKey().length() > word.length()
                        && other.getKey().contains(word)
                        && other.getValue() >= entry.getValue() * 0.7) {
                    isSubstring = true;
                    break;
                }
            }
            if (!isSubstring) {
                filtered.add(word);
            }
        }

        return sortedWords.stream()
                .filter(entry -> filtered.contains(entry.getKey()))
                .limit(100)
                .map(entry -> new WordFrequency(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    private boolean hasText(String text) {
        return text != null && !text.isBlank();
    }

    private String defaultText(String text, String fallback) {
        return hasText(text) ? text : fallback;
    }

    private String truncateText(String text, int maxLength) {
        if (!hasText(text) || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    private String replaceNames(String text, String oldName, String newName, String oldShortName, String newShortName) {
        return text.replace(oldName, newName).replace(oldShortName, newShortName);
    }

    private record OutlineItem(String title, String outline) {}
}
