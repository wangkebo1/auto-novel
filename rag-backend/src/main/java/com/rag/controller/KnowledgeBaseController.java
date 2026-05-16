package com.rag.controller;

import com.rag.dto.KnowledgeBaseRequest;
import com.rag.dto.Result;
import com.rag.entity.KnowledgeBase;
import com.rag.service.KnowledgeBaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    /**
     * GET /api/knowledge-bases
     * 获取所有启用的知识库列表
     */
    @GetMapping
    public Result<List<KnowledgeBase>> list() {
        return Result.ok(knowledgeBaseService.listAll());
    }

    /**
     * POST /api/knowledge-bases
     * 创建新知识库
     */
    @PostMapping
    public Result<KnowledgeBase> create(@Valid @RequestBody KnowledgeBaseRequest request) {
        KnowledgeBase kb = knowledgeBaseService.create(request);
        return Result.ok(kb);
    }

    /**
     * GET /api/knowledge-bases/{id}
     * 查询知识库详情
     */
    @GetMapping("/{id}")
    public Result<KnowledgeBase> getById(@PathVariable Long id) {
        return Result.ok(knowledgeBaseService.getById(id));
    }

    /**
     * DELETE /api/knowledge-bases/{id}
     * 禁用知识库（软删除）
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        knowledgeBaseService.delete(id);
        return Result.ok();
    }
}
