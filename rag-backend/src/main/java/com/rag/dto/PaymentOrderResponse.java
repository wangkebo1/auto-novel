package com.rag.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PaymentOrderResponse {
    private Long id;
    private String orderNo;
    private String packageName;
    private Integer points;
    private Integer amountCents;
    private String status;
    private String paymentChannel;
    private Boolean refundable;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
