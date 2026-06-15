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

/**
 * Consumes order lifecycle events and dispatches email notifications.
 *
 * NOTE: recipientEmail is expected in the event payload.
 * In production it would be fetched from a user-profile service or embedded
 * in the event by the service that emits it.
 */
@PersistenceAdapter
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final SendNotificationUseCase sendNotificationUseCase;
    private final EmailTemplateService    emailTemplateService;

    @Value("${notification.admin-email:admin@bookplus.com}")
    private String adminEmail;

    // ── order.status.changed ──────────────────────────────────────────────

    @KafkaListener(topics = "order.status.changed", groupId = "notification-service-order",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onOrderStatusChanged(
            @Payload Map<String, Object> payload,
            @Header(KafkaHeaders.RECEIVED_KEY) String key
    ) {
        String orderId     = (String) payload.get("orderId");
        String userId      = (String) payload.get("userId");
        String newStatus   = (String) payload.get("newStatus");
        String email       = extractEmail(payload, adminEmail);

        log.info("order.status.changed orderId={} newStatus={}", orderId, newStatus);

        try {
            String subject;
            String body;

            subject = switch (newStatus) {
                case "CONFIRMED"  -> "Order Confirmed! — " + orderId;
                case "SHIPPED"    -> "Your Order Has Shipped! — " + orderId;
                case "DELIVERED"  -> "Order Delivered — " + orderId;
                case "CANCELLED"  -> "Order Cancelled — " + orderId;
                case "REFUNDED"   -> "Order Refunded — " + orderId;
                default           -> null;
            };
            if (subject == null) return; // PENDING_PAYMENT / PAYMENT_PROCESSING → no email

            body = switch (newStatus) {
                case "CONFIRMED"  -> emailTemplateService.orderConfirmed(orderId);
                case "SHIPPED"    -> emailTemplateService.orderShipped(orderId);
                case "DELIVERED"  -> emailTemplateService.orderDelivered(orderId);
                case "CANCELLED"  -> emailTemplateService.orderCancelled(orderId,
                                         (String) payload.getOrDefault("reason", "N/A"));
                case "REFUNDED"   -> emailTemplateService.orderRefunded(orderId,
                                         (String) payload.getOrDefault("reason", "Reembolso emitido"));
                default           -> "";
            };

            NotificationType type = switch (newStatus) {
                case "CONFIRMED"  -> NotificationType.ORDER_CONFIRMED;
                case "SHIPPED"    -> NotificationType.ORDER_SHIPPED;
                case "DELIVERED"  -> NotificationType.ORDER_DELIVERED;
                case "CANCELLED"  -> NotificationType.ORDER_CANCELLED;
                case "REFUNDED"   -> NotificationType.ORDER_REFUNDED;
                default           -> NotificationType.ORDER_CREATED;
            };

            sendNotificationUseCase.send(new SendNotificationCommand(
                    userId, email, type, NotificationChannel.EMAIL, subject, body, orderId));

        } catch (Exception ex) {
            log.error("Failed to send notification for order.status.changed orderId={}: {}",
                    orderId, ex.getMessage());
        }
    }

    // ── order.created ─────────────────────────────────────────────────────

    @KafkaListener(topics = "order.created", groupId = "notification-service-order",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onOrderCreated(
            @Payload Map<String, Object> payload,
            @Header(KafkaHeaders.RECEIVED_KEY) String key
    ) {
        String orderId = (String) payload.get("orderId");
        String userId  = (String) payload.get("userId");
        String email   = extractEmail(payload, adminEmail);

        @SuppressWarnings("unchecked")
        Map<String, Object> total = (Map<String, Object>) payload.getOrDefault("total",
                Map.of("amount", "0.00", "currency", "USD"));

        log.info("order.created orderId={}", orderId);
        try {
            sendNotificationUseCase.send(new SendNotificationCommand(
                    userId, email,
                    NotificationType.ORDER_CREATED,
                    NotificationChannel.EMAIL,
                    "Order Received — " + orderId,
                    emailTemplateService.orderCreated(orderId,
                            total.get("amount").toString(),
                            (String) total.getOrDefault("currency", "USD")),
                    orderId
            ));
        } catch (Exception ex) {
            log.error("Failed to send order.created notification orderId={}: {}", orderId, ex.getMessage());
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private String extractEmail(Map<String, Object> payload, String fallback) {
        Object email = payload.get("recipientEmail");
        return email != null ? email.toString() : fallback;
    }
}
