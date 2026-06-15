package com.bookplus.payment.application.usecase;

import com.bookplus.payment.application.payment.PaymentMethodResolver;
import com.bookplus.payment.domain.exception.PaymentAlreadyExistsException;
import com.bookplus.payment.domain.model.*;
import com.bookplus.payment.domain.port.in.InitiatePaymentUseCase;
import com.bookplus.payment.domain.port.out.*;
import com.bookplus.payment.domain.service.PaymentAuthorization;
import com.bookplus.payment.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase @RequiredArgsConstructor @Slf4j
public class InitiatePaymentUseCaseImpl implements InitiatePaymentUseCase {

    private final LoadPaymentPort          loadPaymentPort;
    private final SavePaymentPort          savePaymentPort;
    private final DomainEventPublisherPort eventPublisher;
    private final PaymentMethodResolver    paymentMethodResolver;

    @Override
    public Payment initiate(InitiatePaymentCommand cmd) {
        // Guard: only one active payment per order
        loadPaymentPort.findByOrderId(cmd.orderId()).ifPresent(existing -> {
            throw new PaymentAlreadyExistsException(cmd.orderId());
        });

        Money   amount  = Money.of(cmd.amount(), cmd.currency());
        Payment payment = Payment.initiate(cmd.orderId(), cmd.userId(), amount, cmd.paymentMethod());

        // Move to PROCESSING (in a real system this happens after gateway ACK)
        payment.process();

        // Strategy: authorize against the method-specific (simulated) gateway,
        // then settle the payment synchronously so the order saga can advance.
        PaymentAuthorization auth = paymentMethodResolver.resolve(cmd.paymentMethod()).authorize(payment);
        if (auth.approved()) {
            payment.complete(auth.transactionRef());
        } else {
            payment.fail(auth.declineReason());
        }

        Payment saved = savePaymentPort.save(payment);

        // Publishes PaymentInitiatedEvent + PaymentCompleted/Failed in one shot.
        // order-service handles these idempotently and tolerates out-of-order delivery.
        try {
            eventPublisher.publishAll(payment.pullDomainEvents());
        } catch (Exception ex) {
            log.error("CRITICAL — failed to publish payment events for order {}: {}",
                    cmd.orderId(), ex.getMessage());
            throw new RuntimeException("Payment event publication failed", ex);
        }

        log.info("Payment {} for order {} settled as {} via {} — amount {}{}",
                saved.getId(), cmd.orderId(), saved.getStatus(), cmd.paymentMethod(),
                amount.amount(), amount.currency());
        return saved;
    }
}
