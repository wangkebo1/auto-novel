package com.rag.service.impl;

import com.rag.dto.KnowledgeBaseRequest;
import com.rag.entity.KnowledgeBase;
import com.rag.repository.KnowledgeBaseRepository;
import com.rag.security.SecurityUtils;
import com.rag.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final SecurityUtils securityUtils;

    @Override
    public List<KnowledgeBase> listAll() {
        Long userId = securityUtils.getCurrentUserId();
        return knowledgeBaseRepository.findByUserId(userId);
    }

    @Override
    @Transactional
    public KnowledgeBase create(KnowledgeBaseRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        if (knowledgeBaseRepository.existsByNameAndUserId(request.getName(), userId)) {
            throw new RuntimeException("知识库名称已存在: " + request.getName());
        }
        KnowledgeBase kb = new KnowledgeBase(request.getName(), request.getDescription());
        kb.setUserId(userId);  // 设置所有者
        knowledgeBaseRepository.save(kb);
        log.info("知识库创建成功: userId={}, id={}, name={}", userId, kb.getId(), kb.getName());
        return kb;
    }

    @Override
    public KnowledgeBase getById(Long id) {
        Long userId = securityUtils.getCurrentUserId();
        return knowledgeBaseRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("知识库不存在或无权访问: " + id));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        KnowledgeBase kb = getById(id);  // getById 已包含权限检查
        kb.setEnabled(false);
        knowledgeBaseRepository.save(kb);
        log.info("知识库已禁用: userId={}, id={}, name={}", kb.getUserId(), id, kb.getName());
    }
}
