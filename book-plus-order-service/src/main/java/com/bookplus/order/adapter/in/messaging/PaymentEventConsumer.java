package com.bookplus.order.adapter.in.messaging;

import com.bookplus.order.domain.port.in.UpdateOrderStatusUseCase;
import com.bookplus.order.shared.annotation.PersistenceAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Consumes payment events from payment-service.
 *
 * Topics:
 *  - payment.initiated  → order transitions to PAYMENT_PROCESSING
 *  - payment.confirmed  → order transitions to CONFIRMED
 *  - payment.failed     → order is cancelled (payment failed)
 */
@PersistenceAdapter
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final UpdateOrderStatusUseCase updateStatusUseCase;
    private final IdempotencyGuard         idempotencyGuard;

    @KafkaListener(topics = "payment.initiated", groupId = "order-service-payment",
                   containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void onPaymentInitiated(
            @Payload Map<String, Object> payload,
            @Header(KafkaHeaders.RECEIVED_KEY) String key
    ) {
        if (!idempotencyGuard.tryAcquire(key, "payment.initiated")) return;
        String orderId   = (String) payload.get("orderId");
        String paymentId = (String) payload.get("paymentId");
        log.info("Payment initiated — orderId={} paymentId={}", orderId, paymentId);
        try {
            updateStatusUseCase.startPaymentProcessing(orderId, paymentId);
        } catch (Exception ex) {
            log.error("Failed to process payment.initiated for orderId={}: {}", orderId, ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    @KafkaListener(topics = "payment.confirmed", groupId = "order-service-payment",
                   containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void onPaymentConfirmed(
            @Payload Map<String, Object> payload,
            @Header(KafkaHeaders.RECEIVED_KEY) String key
    ) {
        if (!idempotencyGuard.tryAcquire(key, "payment.confirmed")) return;
        String orderId = (String) payload.get("orderId");
        log.info("Payment confirmed — orderId={}", orderId);
        try {
            updateStatusUseCase.confirmPayment(orderId);
        } catch (Exception ex) {
            log.error("Failed to process payment.confirmed for orderId={}: {}", orderId, ex.getMessage());
            throw new RuntimeException(ex);
        }
    }
}
