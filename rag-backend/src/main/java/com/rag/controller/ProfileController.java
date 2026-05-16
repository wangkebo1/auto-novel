package com.rag.controller;

import com.rag.dto.*;
import com.rag.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final BillingService billingService;

    @GetMapping
    public Result<ProfileResponse> getProfile() {
        return Result.ok(billingService.getProfile());
    }

    @GetMapping("/packages")
    public Result<List<PaymentPackageResponse>> listPackages() {
        return Result.ok(billingService.listPackages());
    }

    @PostMapping("/orders")
    public Result<PaymentOrderResponse> createOrder(@RequestBody CreatePaymentOrderRequest request) {
        return Result.ok(billingService.createOrder(request));
    }

    @PostMapping("/orders/{orderId}/pay")
    public Result<PaymentOrderResponse> payOrder(@PathVariable Long orderId) {
        return Result.ok(billingService.payOrder(orderId));
    }

    @GetMapping("/orders")
    public Result<List<PaymentOrderResponse>> listOrders() {
        return Result.ok(billingService.listMyOrders());
    }

    @PostMapping("/orders/{orderId}/refund")
    public Result<RefundOrderResponse> requestRefund(@PathVariable Long orderId, @RequestBody RefundRequest request) {
        return Result.ok(billingService.requestRefund(orderId, request));
    }

    @GetMapping("/refunds")
    public Result<List<RefundOrderResponse>> listRefunds() {
        return Result.ok(billingService.listMyRefunds());
    }

    @GetMapping("/points/transactions")
    public Result<List<PointTransactionResponse>> listPointTransactions() {
        return Result.ok(billingService.listMyPointTransactions());
    }
}
