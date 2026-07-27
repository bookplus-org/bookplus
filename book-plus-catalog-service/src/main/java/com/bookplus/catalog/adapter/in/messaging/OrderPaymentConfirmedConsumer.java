package com.bookplus.catalog.adapter.in.messaging;

import com.bookplus.catalog.domain.port.in.PurchaseAccessUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Proyecta las compras confirmadas para dar acceso al PDF completo (biblioteca del usuario).
 * Solo parsea el evento Kafka; la lógica de concesión vive en {@link PurchaseAccessUseCase}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPaymentConfirmedConsumer {

    private final PurchaseAccessUseCase purchaseAccessUseCase;

    @KafkaListener(topics = "order.payment.confirmed", groupId = "catalog-service",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onPaymentConfirmed(Map<String, Object> payload) {
        String userId = asString(payload.get("userId"));
        if (userId == null) {
            return;
        }
        // Solo las compras DIGITALES conceden acceso a la biblioteca (descarga/lectura).
        String deliveryType = asString(payload.getOrDefault("deliveryType", "PHYSICAL"));
        if (!"DIGITAL".equalsIgnoreCase(deliveryType)) {
            log.info("Order for user {} is {} — no library access granted", userId, deliveryType);
            return;
        }
        for (Map<String, Object> item : items(payload)) {
            String bookId = asString(item.get("bookId"));
            if (bookId == null) {
                continue;
            }
            try {
                purchaseAccessUseCase.grantAccess(userId, bookId);
            } catch (Exception ex) {
                log.warn("Could not register purchase user={} book={}: {}", userId, bookId, ex.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> items(Map<String, Object> payload) {
        Object raw = payload.get("items");
        return raw instanceof List ? (List<Map<String, Object>>) raw : Collections.emptyList();
    }

    private static String asString(Object o) {
        if (o instanceof Map<?, ?> m && m.containsKey("value")) {
            return String.valueOf(m.get("value"));
        }
        return o == null ? null : o.toString();
    }
}
