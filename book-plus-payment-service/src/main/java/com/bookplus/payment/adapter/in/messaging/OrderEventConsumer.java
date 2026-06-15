package com.bookplus.payment.adapter.in.messaging;

import com.bookplus.payment.domain.model.PaymentMethod;
import com.bookplus.payment.domain.port.in.*;
import com.bookplus.payment.domain.port.in.InitiatePaymentUseCase.InitiatePaymentCommand;
import com.bookplus.payment.shared.annotation.PersistenceAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Consumes order lifecycle events.
 *
 * order.created   → initiates a payment (PENDING_PAYMENT state trigger)
 * order.cancelled → triggers refund if payment was COMPLETED
 */
@PersistenceAdapter
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final InitiatePaymentUseCase initiatePaymentUseCase;
    private final RefundPaymentUseCase   refundPaymentUseCase;

    @KafkaListener(topics = "order.created", groupId = "payment-service-order",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onOrderCreated(
            @Payload Map<String, Object> payload,
            @Header(KafkaHeaders.RECEIVED_KEY) String key
    ) {
        String orderId = (String) payload.get("orderId");
        String userId  = (String) payload.get("userId");
        log.info("Received order.created for orderId={}", orderId);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> totalMap = (Map<String, Object>) payload.get("total");
            BigDecimal amount   = new BigDecimal(totalMap.get("amount").toString());
            String     currency = (String) totalMap.getOrDefault("currency", "USD");

            // Default to CREDIT_CARD; in real flow this comes from user's saved payment method
            String rawMethod = (String) payload.getOrDefault("paymentMethod", "CREDIT_CARD");
            PaymentMethod method = PaymentMethod.valueOf(rawMethod.toUpperCase());

            initiatePaymentUseCase.initiate(new InitiatePaymentCommand(
                    orderId, userId, amount, currency, method));

        } catch (Exception ex) {
            log.error("Failed to process order.created for orderId={}: {}", orderId, ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    @KafkaListener(topics = "order.cancelled", groupId = "payment-service-order",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onOrderCancelled(
            @Payload Map<String, Object> payload,
            @Header(KafkaHeaders.RECEIVED_KEY) String key
    ) {
        String orderId = (String) payload.get("orderId");
        String reason  = (String) payload.getOrDefault("reason", "Order cancelled");
        log.info("Received order.cancelled for orderId={}", orderId);

        try {
            // Only refund if a completed payment exists — use case handles missing payment gracefully
            refundPaymentUseCase.refund(orderId, reason);
        } catch (Exception ex) {
            // Non-fatal: order may have been cancelled before payment was completed
            log.warn("No refundable payment found for orderId={}: {}", orderId, ex.getMessage());
        }
    }
}
