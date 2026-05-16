package com.rag.repository;

import com.rag.entity.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    List<PaymentOrder> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<PaymentOrder> findByIdAndUserId(Long id, Long userId);

    Optional<PaymentOrder> findByOrderNo(String orderNo);
}
