package com.rag.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "payment_orders", indexes = {
        @Index(name = "idx_payment_orders_user_id", columnList = "user_id"),
        @Index(name = "idx_payment_orders_status", columnList = "status")
})
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "order_no", nullable = false, unique = true, length = 64)
    private String orderNo;

    @Column(name = "package_name", nullable = false, length = 100)
    private String packageName;

    @Column(nullable = false)
    private Integer points;

    @Column(name = "amount_cents", nullable = false)
    private Integer amountCents;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "payment_channel", nullable = false, length = 30)
    private String paymentChannel = "MOCK";

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
