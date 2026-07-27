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
 * Revoca el acceso a la biblioteca cuando se reembolsa un pedido DIGITAL
 * (evento order.access.revoked emitido por order-service). Solo parsea el evento;
 * la lógica de revocación vive en {@link PurchaseAccessUseCase}. Idempotente.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DigitalAccessRevokedConsumer {

    private final PurchaseAccessUseCase purchaseAccessUseCase;

    @KafkaListener(topics = "order.access.revoked", groupId = "catalog-service",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onAccessRevoked(Map<String, Object> payload) {
        String userId = asString(payload.get("userId"));
        if (userId == null) {
            return;
        }
        for (Map<String, Object> item : items(payload)) {
            String bookId = asString(item.get("bookId"));
            if (bookId == null) {
                continue;
            }
            try {
                purchaseAccessUseCase.revokeAccess(userId, bookId);
            } catch (Exception ex) {
                log.warn("Could not revoke access user={} book={}: {}", userId, bookId, ex.getMessage());
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
