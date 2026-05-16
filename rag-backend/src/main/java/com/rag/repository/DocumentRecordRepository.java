package com.rag.repository;

import com.rag.entity.DocumentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRecordRepository extends JpaRepository<DocumentRecord, Long> {

    boolean existsByFileHashAndStatus(String fileHash, String status);

    Optional<DocumentRecord> findByFileHash(String fileHash);

    List<DocumentRecord> findByKnowledgeBaseId(Long knowledgeBaseId);

    List<DocumentRecord> findByStatus(String status);

    long countByKnowledgeBaseId(Long knowledgeBaseId);
}
