package com.rag.repository;

import com.rag.entity.NovelCharacter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NovelCharacterRepository extends JpaRepository<NovelCharacter, Long> {
    List<NovelCharacter> findByNovelIdOrderByIdAsc(Long novelId);
    Optional<NovelCharacter> findByIdAndNovelId(Long id, Long novelId);
}
