package com.rag.repository;

import com.rag.entity.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {

    List<KnowledgeBase> findByEnabledTrue();

    boolean existsByName(String name);

    /** 检查用户是否已有同名知识库 */
    boolean existsByNameAndUserId(String name, Long userId);

    /** 查询用户的所有知识库 */
    List<KnowledgeBase> findByUserId(Long userId);

    /** 查询用户的指定知识库 */
    Optional<KnowledgeBase> findByIdAndUserId(Long id, Long userId);
}
