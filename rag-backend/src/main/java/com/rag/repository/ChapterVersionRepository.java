package com.rag.repository;

import com.rag.entity.ChapterVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChapterVersionRepository extends JpaRepository<ChapterVersion, Long> {
    List<ChapterVersion> findByChapterIdOrderByVersionNumberDesc(Long chapterId);

    Optional<ChapterVersion> findTopByChapterIdOrderByVersionNumberDesc(Long chapterId);

    Optional<ChapterVersion> findByIdAndChapterId(Long id, Long chapterId);
}
