package com.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentPackageResponse {
    private String code;
    private String name;
    private Integer points;
    private Integer amountCents;
}
