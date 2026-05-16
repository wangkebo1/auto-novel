package com.rag.repository;

import com.rag.entity.PointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {
    List<PointTransaction> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);
}
