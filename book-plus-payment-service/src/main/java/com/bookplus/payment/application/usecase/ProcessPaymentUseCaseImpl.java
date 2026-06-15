package com.bookplus.payment.application.usecase;

import com.bookplus.payment.domain.exception.PaymentNotFoundException;
import com.bookplus.payment.domain.model.Payment;
import com.bookplus.payment.domain.port.in.ProcessPaymentUseCase;
import com.bookplus.payment.domain.port.out.*;
import com.bookplus.payment.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles gateway webhook callbacks.
 * In production these endpoints would be called by the payment gateway (e.g. Stripe webhooks).
 * The webhook signature validation would live in the web adapter.
 */
@UseCase @RequiredArgsConstructor @Slf4j
public class ProcessPaymentUseCaseImpl implements ProcessPaymentUseCase {

    private final LoadPaymentPort          loadPaymentPort;
    private final SavePaymentPort          savePaymentPort;
    private final DomainEventPublisherPort eventPublisher;

    @Override
    public Payment complete(String paymentId, String transactionRef) {
        Payment payment = load(paymentId);
        payment.complete(transactionRef);
        Payment saved = savePaymentPort.save(payment);

        // PaymentCompletedEvent → order-service (confirmPayment) + notification-service
        publishCritical(payment, paymentId);
        log.info("Payment {} COMPLETED — txRef={}", paymentId, transactionRef);
        return saved;
    }

    @Override
    public Payment fail(String paymentId, String reason) {
        Payment payment = load(paymentId);
        payment.fail(reason);
        Payment saved = savePaymentPort.save(payment);

        // PaymentFailedEvent → order-service (cancel order) + notification-service
        publishCritical(payment, paymentId);
        log.warn("Payment {} FAILED — reason={}", paymentId, reason);
        return saved;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Payment load(String paymentId) {
        return loadPaymentPort.findByPaymentId(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }

    private void publishCritical(Payment payment, String paymentId) {
        try {
            eventPublisher.publishAll(payment.pullDomainEvents());
        } catch (Exception ex) {
            log.error("CRITICAL — failed to publish payment event for paymentId {}: {}", paymentId, ex.getMessage());
            throw new RuntimeException("Payment event publication failed", ex);
        }
    }
}
