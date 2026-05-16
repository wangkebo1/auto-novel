package com.rag.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PointTransactionResponse {
    private Long id;
    private String type;
    private Integer changeAmount;
    private Integer balanceAfter;
    private String sourceType;
    private Long sourceId;
    private String description;
    private LocalDateTime createdAt;
}
