package com.rag.service;

import com.rag.dto.*;

import java.util.List;

public interface BillingService {
    ProfileResponse getProfile();

    List<PaymentPackageResponse> listPackages();

    PaymentOrderResponse createOrder(CreatePaymentOrderRequest request);

    PaymentOrderResponse payOrder(Long orderId);

    List<PaymentOrderResponse> listMyOrders();

    RefundOrderResponse requestRefund(Long orderId, RefundRequest request);

    List<RefundOrderResponse> listMyRefunds();

    List<PointTransactionResponse> listMyPointTransactions();

    List<RefundOrderResponse> listAllRefunds();

    RefundOrderResponse reviewRefund(Long refundId, RefundReviewRequest request);

    void consumePoints(Long userId, int amount, String sourceType, Long sourceId, String description);

    boolean isAdmin(Long userId);
}
