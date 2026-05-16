package com.rag.service;

import com.rag.dto.KnowledgeBaseRequest;
import com.rag.entity.KnowledgeBase;

import java.util.List;

public interface KnowledgeBaseService {

    List<KnowledgeBase> listAll();

    KnowledgeBase create(KnowledgeBaseRequest request);

    KnowledgeBase getById(Long id);

    void delete(Long id);
}
