package com.rag.dto;

import lombok.Data;

@Data
public class RefundReviewRequest {
    private Boolean approved;
    private String reviewerNote;
}
