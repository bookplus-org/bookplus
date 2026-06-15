package com.bookplus.notification.adapter.in.messaging;

import com.bookplus.notification.application.service.EmailTemplateService;
import com.bookplus.notification.domain.model.*;
import com.bookplus.notification.domain.port.in.SendNotificationUseCase;
import com.bookplus.notification.domain.port.in.SendNotificationUseCase.SendNotificationCommand;
import com.bookplus.notification.shared.annotation.PersistenceAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

import java.util.Map;

@PersistenceAdapter
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final SendNotificationUseCase sendNotificationUseCase;
    private final EmailTemplateService    emailTemplateService;

    @Value("${notification.admin-email:admin@bookplus.com}")
    private String adminEmail;

    @KafkaListener(topics = "payment.confirmed", groupId = "notification-service-payment",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onPaymentCompleted(@Payload Map<String, Object> payload,
                                   @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        String paymentId  = (String) payload.get("paymentId");
        String orderId    = (String) payload.get("orderId");
        String userId     = (String) payload.get("userId");
        String email      = extractEmail(payload);
        String txRef      = (String) payload.getOrDefault("transactionRef", "N/A");

        @SuppressWarnings("unchecked")
        Map<String, Object> amount = (Map<String, Object>) payload.getOrDefault("amount",
                Map.of("amount", "0.00", "currency", "USD"));

        log.info("payment.confirmed paymentId={}", paymentId);
        try {
            sendNotificationUseCase.send(new SendNotificationCommand(
                    userId, email,
                    NotificationType.PAYMENT_COMPLETED,
                    NotificationChannel.EMAIL,
                    "Payment Successful — Order " + orderId,
                    emailTemplateService.paymentCompleted(orderId,
                            amount.get("amount").toString(),
                            (String) amount.getOrDefault("currency", "USD"), txRef),
                    paymentId
            ));
        } catch (Exception ex) {
            log.error("Failed to send payment.confirmed notification paymentId={}: {}", paymentId, ex.getMessage());
        }
    }

    @KafkaListener(topics = "payment.failed", groupId = "notification-service-payment",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onPaymentFailed(@Payload Map<String, Object> payload,
                                @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        String paymentId = (String) payload.get("paymentId");
        String orderId   = (String) payload.get("orderId");
        String userId    = (String) payload.get("userId");
        String reason    = (String) payload.getOrDefault("reason", "Unknown reason");
        String email     = extractEmail(payload);

        log.info("payment.failed paymentId={}", paymentId);
        try {
            sendNotificationUseCase.send(new SendNotificationCommand(
                    userId, email,
                    NotificationType.PAYMENT_FAILED,
                    NotificationChannel.EMAIL,
                    "Payment Failed — Order " + orderId,
                    emailTemplateService.paymentFailed(orderId, reason),
                    paymentId
            ));
        } catch (Exception ex) {
            log.error("Failed to send payment.failed notification paymentId={}: {}", paymentId, ex.getMessage());
        }
    }

    @KafkaListener(topics = "payment.refunded", groupId = "notification-service-payment",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onPaymentRefunded(@Payload Map<String, Object> payload,
                                  @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        String paymentId = (String) payload.get("paymentId");
        String orderId   = (String) payload.get("orderId");
        String userId    = (String) payload.get("userId");
        String email     = extractEmail(payload);

        @SuppressWarnings("unchecked")
        Map<String, Object> refund = (Map<String, Object>) payload.getOrDefault("refundAmount",
                Map.of("amount", "0.00", "currency", "USD"));

        log.info("payment.refunded paymentId={}", paymentId);
        try {
            sendNotificationUseCase.send(new SendNotificationCommand(
                    userId, email,
                    NotificationType.PAYMENT_REFUNDED,
                    NotificationChannel.EMAIL,
                    "Refund Initiated — Order " + orderId,
                    emailTemplateService.refundInitiated(orderId,
                            refund.get("amount").toString(),
                            (String) refund.getOrDefault("currency", "USD")),
                    paymentId
            ));
        } catch (Exception ex) {
            log.error("Failed to send payment.refunded notification paymentId={}: {}", paymentId, ex.getMessage());
        }
    }

    private String extractEmail(Map<String, Object> payload) {
        Object email = payload.get("recipientEmail");
        return email != null ? email.toString() : adminEmail;
    }
}
