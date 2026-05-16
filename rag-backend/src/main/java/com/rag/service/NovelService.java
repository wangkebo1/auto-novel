package com.rag.service;

import com.rag.dto.*;
import reactor.core.publisher.Flux;

import java.util.List;

public interface NovelService {

    /** 鍒涘缓灏忚椤圭洰 */
    NovelResponse createNovel(NovelRequest request);

    /** 鑾峰彇褰撳墠鐢ㄦ埛鐨勫皬璇村垪琛?*/
    List<NovelResponse> listMyNovels();

    /** 鑾峰彇灏忚璇︽儏锛堝惈绔犺妭鍜岃鑹诧級 */
    NovelResponse getNovel(Long novelId);

    /** 鏇存柊灏忚鍩烘湰淇℃伅 */
    NovelResponse updateNovel(Long novelId, NovelRequest request);

    /** 鍒犻櫎灏忚 */
    void deleteNovel(Long novelId);

    /** 娣诲姞瑙掕壊 */
    NovelResponse.CharacterInfo addCharacter(Long novelId, CharacterRequest request);

    /** 鏇存柊瑙掕壊 */
    NovelResponse.CharacterInfo updateCharacter(Long novelId, Long characterId, CharacterRequest request);

    /** 鏇存柊瑙掕壊骞舵浛鎹㈢珷鑺傚唴瀹?*/
    NovelResponse.CharacterInfo updateCharacterWithReplace(Long novelId, Long characterId, String oldName, CharacterRequest request);

    /** 鍒犻櫎瑙掕壊 */
    void deleteCharacter(Long novelId, Long characterId);

    /** 鍒犻櫎鍗曚釜绔犺妭 */
    void deleteChapter(Long novelId, Long chapterId);

    /** 鍒犻櫎鎵€鏈夊ぇ绾茬珷鑺傦紙鍙垹OUTLINE鐘舵€佺殑锛?*/
    void deleteOutlineChapters(Long novelId);

    /** AI 鐢熸垚鍏ㄤ功澶х翰锛堣繑鍥炵珷鑺傚垪琛級 */
    List<NovelResponse.ChapterBrief> generateOutline(Long novelId, Integer chapterCount);

    /** 鑾峰彇绔犺妭璇︽儏 */
    ChapterResponse getChapter(Long novelId, Long chapterId);

    /** 鏇存柊绔犺妭锛堢敤鎴风紪杈戯級 */
    ChapterResponse updateChapter(Long novelId, Long chapterId, String title, String outline, String content);

    /** 鏇存柊绔犺妭澶囨敞 */
    ChapterResponse updateChapterNotes(Long novelId, Long chapterId, String notes);

    /** AI 提供章节后续分支建议 */
    List<BranchSuggestionResponse> suggestChapterBranches(Long novelId, Long chapterId);

    /** AI 鐢熸垚绔犺妭鍐呭锛堝悓姝ワ級 */
    ChapterResponse generateChapter(Long novelId, Long chapterId, GenerateRequest request);

    /** AI 鐢熸垚绔犺妭鍐呭锛圫SE 娴佸紡锛?*/
    Flux<String> generateChapterStream(Long novelId, Long chapterId, GenerateRequest request);

    /** 瀵煎嚭灏忚涓虹函鏂囨湰 */
    String exportNovelAsText(Long novelId);

    /** 瀵煎嚭灏忚涓?Markdown */
    String exportNovelAsMarkdown(Long novelId);

    /** 鑾峰彇灏忚缁熻鏁版嵁 */
    NovelStatistics getStatistics(Long novelId);

    /** 鑾峰彇璇嶉鍒嗘瀽 */
    List<WordFrequency> getWordFrequency(Long novelId);
}
