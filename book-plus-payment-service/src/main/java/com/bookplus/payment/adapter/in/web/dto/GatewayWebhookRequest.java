package com.bookplus.payment.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload from the payment gateway webhook.
 * In production this would carry a signature header validated in the controller.
 */
public record GatewayWebhookRequest(
        @NotBlank String paymentId,
        @NotBlank String status,           // "completed" or "failed"
        String transactionRef,             // present when status = completed
        String failureReason               // present when status = failed
) {}
