package com.rag.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RefundOrderResponse {
    private Long id;
    private Long paymentOrderId;
    private String refundNo;
    private String reason;
    private String status;
    private String reviewerNote;
    private LocalDateTime refundedAt;
    private LocalDateTime createdAt;
}
