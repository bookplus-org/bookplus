package com.bookplus.payment.application.usecase;

import com.bookplus.payment.domain.exception.*;
import com.bookplus.payment.domain.model.Payment;
import com.bookplus.payment.domain.port.in.RefundPaymentUseCase;
import com.bookplus.payment.domain.port.out.*;
import com.bookplus.payment.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase @RequiredArgsConstructor @Slf4j
public class RefundPaymentUseCaseImpl implements RefundPaymentUseCase {

    private final LoadPaymentPort          loadPaymentPort;
    private final SavePaymentPort          savePaymentPort;
    private final DomainEventPublisherPort eventPublisher;

    @Override
    public Payment refund(String orderId, String reason) {
        Payment payment = loadPaymentPort.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException("order:" + orderId));

        payment.refund(reason);
        Payment saved = savePaymentPort.save(payment);

        // RefundInitiatedEvent → notification-service (refund confirmation email)
        try {
            eventPublisher.publishAll(payment.pullDomainEvents());
        } catch (Exception ex) {
            log.error("CRITICAL — failed to publish RefundInitiatedEvent for order {}: {}", orderId, ex.getMessage());
            throw new RuntimeException("Refund event publication failed", ex);
        }

        log.info("Refund initiated for payment {} (order {}) — reason: {}", saved.getId(), orderId, reason);
        return saved;
    }
}
