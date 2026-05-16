package com.rag.repository;

import com.rag.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    List<Chapter> findByNovelIdOrderByChapterNumberAsc(Long novelId);
    Optional<Chapter> findByIdAndNovelId(Long id, Long novelId);
    Optional<Chapter> findByNovelIdAndChapterNumber(Long novelId, Integer chapterNumber);
    int countByNovelId(Long novelId);
    void deleteByNovelIdAndStatus(Long novelId, String status);
}
