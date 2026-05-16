package com.rag.service.impl;

import com.rag.dto.*;
import com.rag.entity.PaymentOrder;
import com.rag.entity.PointTransaction;
import com.rag.entity.RefundOrder;
import com.rag.entity.User;
import com.rag.repository.PaymentOrderRepository;
import com.rag.repository.PointTransactionRepository;
import com.rag.repository.RefundOrderRepository;
import com.rag.repository.UserRepository;
import com.rag.security.SecurityUtils;
import com.rag.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BillingServiceImpl implements BillingService {

    private static final List<PaymentPackageResponse> PACKAGES = List.of(
            new PaymentPackageResponse("starter", "\u65b0\u624b 500", 500, 990),
            new PaymentPackageResponse("popular", "\u70ed\u95e8 1200", 1200, 1990),
            new PaymentPackageResponse("pro", "\u9ad8\u7ea7 3000", 3000, 3990)
    );

    private final UserRepository userRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final RefundOrderRepository refundOrderRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getProfile() {
        User user = securityUtils.getCurrentUser();
        return ProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .points(user.getPoints())
                .roles(user.getRoles().stream().map(role -> role.getName()).collect(Collectors.joining(",")))
                .build();
    }

    @Override
    public List<PaymentPackageResponse> listPackages() {
        return PACKAGES;
    }

    @Override
    @Transactional
    public PaymentOrderResponse createOrder(CreatePaymentOrderRequest request) {
        User user = securityUtils.getCurrentUser();
        PaymentPackageResponse pkg = findPackage(request.getPackageCode());

        PaymentOrder order = new PaymentOrder();
        order.setUserId(user.getId());
        order.setOrderNo(buildNo("PO"));
        order.setPackageName(pkg.getName());
        order.setPoints(pkg.getPoints());
        order.setAmountCents(pkg.getAmountCents());
        paymentOrderRepository.save(order);

        return toPaymentOrderResponse(order, true);
    }

    @Override
    @Transactional
    public PaymentOrderResponse payOrder(Long orderId) {
        User user = securityUtils.getCurrentUser();
        PaymentOrder order = paymentOrderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!"PENDING".equals(order.getStatus())) {
            throw new RuntimeException("Only pending orders can be paid");
        }

        order.setStatus("PAID");
        order.setPaidAt(LocalDateTime.now());
        paymentOrderRepository.save(order);

        int nextBalance = user.getPoints() + order.getPoints();
        user.setPoints(nextBalance);
        userRepository.save(user);
        recordTransaction(user.getId(), "INCOME", order.getPoints(), nextBalance, "PAYMENT", order.getId(), "Order paid: " + order.getPackageName());

        return toPaymentOrderResponse(order, isRefundable(order));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentOrderResponse> listMyOrders() {
        Long userId = securityUtils.getCurrentUserId();
        return paymentOrderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(order -> toPaymentOrderResponse(order, isRefundable(order)))
                .toList();
    }

    @Override
    @Transactional
    public RefundOrderResponse requestRefund(Long orderId, RefundRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        PaymentOrder order = paymentOrderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!isRefundable(order)) {
            throw new RuntimeException("Order is not refundable");
        }

        if (refundOrderRepository.findByPaymentOrderId(orderId).isPresent()) {
            throw new RuntimeException("Refund request already exists");
        }

        RefundOrder refund = new RefundOrder();
        refund.setUserId(userId);
        refund.setPaymentOrderId(orderId);
        refund.setRefundNo(buildNo("RF"));
        refund.setReason(request.getReason());
        refundOrderRepository.save(refund);

        return toRefundResponse(refund);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefundOrderResponse> listMyRefunds() {
        Long userId = securityUtils.getCurrentUserId();
        return refundOrderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toRefundResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PointTransactionResponse> listMyPointTransactions() {
        Long userId = securityUtils.getCurrentUserId();
        return pointTransactionRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toPointTransactionResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefundOrderResponse> listAllRefunds() {
        return refundOrderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toRefundResponse)
                .toList();
    }

    @Override
    @Transactional
    public RefundOrderResponse reviewRefund(Long refundId, RefundReviewRequest request) {
        User reviewer = securityUtils.getCurrentUser();
        if (!isAdmin(reviewer.getId())) {
            throw new RuntimeException("No permission");
        }

        RefundOrder refund = refundOrderRepository.findById(refundId)
                .orElseThrow(() -> new RuntimeException("Refund not found"));

        if (!"PENDING".equals(refund.getStatus())) {
            throw new RuntimeException("Refund already reviewed");
        }

        PaymentOrder order = paymentOrderRepository.findById(refund.getPaymentOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        refund.setReviewerId(reviewer.getId());
        refund.setReviewerNote(request.getReviewerNote());

        if (Boolean.TRUE.equals(request.getApproved())) {
            User owner = userRepository.findById(order.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (owner.getPoints() < order.getPoints()) {
                throw new RuntimeException("Insufficient points for refund");
            }

            int nextBalance = owner.getPoints() - order.getPoints();
            owner.setPoints(nextBalance);
            userRepository.save(owner);

            refund.setStatus("APPROVED");
            refund.setRefundedAt(LocalDateTime.now());
            order.setStatus("REFUNDED");
            paymentOrderRepository.save(order);

            recordTransaction(owner.getId(), "EXPENSE", -order.getPoints(), nextBalance, "REFUND", refund.getId(), "Refund approved: " + order.getPackageName());
        } else {
            refund.setStatus("REJECTED");
        }

        refundOrderRepository.save(refund);
        return toRefundResponse(refund);
    }

    @Override
    @Transactional
    public void consumePoints(Long userId, int amount, String sourceType, Long sourceId, String description) {
        if (amount <= 0 || isAdmin(userId)) {
            return;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getPoints() < amount) {
            throw new RuntimeException("Insufficient points");
        }

        int nextBalance = user.getPoints() - amount;
        user.setPoints(nextBalance);
        userRepository.save(user);
        recordTransaction(userId, "EXPENSE", -amount, nextBalance, sourceType, sourceId, description);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAdmin(Long userId) {
        return userRepository.findById(userId)
                .map(user -> user.getRoles().stream().anyMatch(role -> "ROLE_ADMIN".equals(role.getName())))
                .orElse(false);
    }

    private void recordTransaction(Long userId, String type, int changeAmount, int balanceAfter, String sourceType, Long sourceId, String description) {
        PointTransaction tx = new PointTransaction();
        tx.setUserId(userId);
        tx.setType(type);
        tx.setChangeAmount(changeAmount);
        tx.setBalanceAfter(balanceAfter);
        tx.setSourceType(sourceType);
        tx.setSourceId(sourceId);
        tx.setDescription(description);
        pointTransactionRepository.save(tx);
    }

    private PaymentPackageResponse findPackage(String code) {
        return PACKAGES.stream()
                .filter(item -> item.getCode().equals(code))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Package not found"));
    }

    private boolean isRefundable(PaymentOrder order) {
        return "PAID".equals(order.getStatus()) && refundOrderRepository.findByPaymentOrderId(order.getId()).isEmpty();
    }

    private String buildNo(String prefix) {
        return prefix + DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(LocalDateTime.now());
    }

    private PaymentOrderResponse toPaymentOrderResponse(PaymentOrder order, boolean refundable) {
        return PaymentOrderResponse.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .packageName(order.getPackageName())
                .points(order.getPoints())
                .amountCents(order.getAmountCents())
                .status(order.getStatus())
                .paymentChannel(order.getPaymentChannel())
                .refundable(refundable)
                .paidAt(order.getPaidAt())
                .createdAt(order.getCreatedAt())
                .build();
    }

    private RefundOrderResponse toRefundResponse(RefundOrder refund) {
        return RefundOrderResponse.builder()
                .id(refund.getId())
                .paymentOrderId(refund.getPaymentOrderId())
                .refundNo(refund.getRefundNo())
                .reason(refund.getReason())
                .status(refund.getStatus())
                .reviewerNote(refund.getReviewerNote())
                .refundedAt(refund.getRefundedAt())
                .createdAt(refund.getCreatedAt())
                .build();
    }

    private PointTransactionResponse toPointTransactionResponse(PointTransaction tx) {
        return PointTransactionResponse.builder()
                .id(tx.getId())
                .type(tx.getType())
                .changeAmount(tx.getChangeAmount())
                .balanceAfter(tx.getBalanceAfter())
                .sourceType(tx.getSourceType())
                .sourceId(tx.getSourceId())
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
