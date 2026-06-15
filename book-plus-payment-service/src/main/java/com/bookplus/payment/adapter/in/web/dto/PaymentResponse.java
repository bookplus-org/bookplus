package com.bookplus.payment.adapter.in.web.dto;

import com.bookplus.payment.domain.model.Payment;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        String     paymentId,
        String     orderId,
        String     userId,
        String     status,
        BigDecimal amount,
        String     currency,
        String     paymentMethod,
        String     gatewayTransactionRef,
        String     failureReason,
        Instant    createdAt,
        Instant    updatedAt
) {
    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(
                p.getId().toString(),
                p.getOrderId(),
                p.getUserId(),
                p.getStatus().name(),
                p.getAmount().amount(),
                p.getAmount().currency(),
                p.getPaymentMethod().name(),
                p.getGatewayTransactionRef(),
                p.getFailureReason(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
