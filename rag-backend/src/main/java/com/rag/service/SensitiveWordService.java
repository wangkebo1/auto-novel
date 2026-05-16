package com.rag.service;

import com.rag.entity.SensitiveWord;
import com.rag.repository.SensitiveWordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SensitiveWordService {

    private final SensitiveWordRepository repository;

    public List<String> detectSensitiveWords(String content) {
        if (content == null || content.isEmpty()) {
            return Collections.emptyList();
        }

        List<SensitiveWord> allWords = repository.findAll();
        List<String> found = new ArrayList<>();

        for (SensitiveWord sw : allWords) {
            if (content.contains(sw.getWord())) {
                found.add(sw.getWord());
            }
        }
        return found;
    }

    public boolean hasSensitiveWords(String content) {
        return !detectSensitiveWords(content).isEmpty();
    }
}
