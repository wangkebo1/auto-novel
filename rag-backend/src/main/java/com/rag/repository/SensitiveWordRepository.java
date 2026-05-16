package com.rag.repository;

import com.rag.entity.SensitiveWord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SensitiveWordRepository extends JpaRepository<SensitiveWord, Long> {
    Optional<SensitiveWord> findByWord(String word);
    List<SensitiveWord> findByCategory(String category);
}
