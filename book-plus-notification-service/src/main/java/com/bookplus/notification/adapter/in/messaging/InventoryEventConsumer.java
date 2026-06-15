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
 * Consumes inventory.stock.low-alert events.
 * These are internal operational alerts — sent to the admin team, not customers.
 */
@PersistenceAdapter
@RequiredArgsConstructor
@Slf4j
public class InventoryEventConsumer {

    private final SendNotificationUseCase sendNotificationUseCase;
    private final EmailTemplateService    emailTemplateService;

    @Value("${notification.admin-email:admin@bookplus.com}")
    private String adminEmail;

    @KafkaListener(topics = "inventory.stock.low-alert", groupId = "notification-service-inventory",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onLowStockAlert(@Payload Map<String, Object> payload,
                                @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        String bookId    = (String) payload.get("bookId");
        String bookTitle = (String) payload.getOrDefault("bookTitle", "Unknown");
        int    available = ((Number) payload.getOrDefault("quantityAvailable", 0)).intValue();

        log.warn("Low stock alert received bookId={} available={}", bookId, available);
        try {
            sendNotificationUseCase.send(new SendNotificationCommand(
                    "system",
                    adminEmail,
                    NotificationType.LOW_STOCK_ALERT,
                    NotificationChannel.EMAIL,
                    "[ALERT] Low Stock: " + bookTitle,
                    emailTemplateService.lowStockAlert(bookTitle, bookId, available),
                    bookId
            ));
        } catch (Exception ex) {
            log.error("Failed to send low-stock alert for bookId={}: {}", bookId, ex.getMessage());
        }
    }
}
