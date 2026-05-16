package com.rag.repository;

import com.rag.entity.RefundOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefundOrderRepository extends JpaRepository<RefundOrder, Long> {
    List<RefundOrder> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<RefundOrder> findAllByOrderByCreatedAtDesc();

    Optional<RefundOrder> findByIdAndUserId(Long id, Long userId);

    Optional<RefundOrder> findByPaymentOrderId(Long paymentOrderId);
}
