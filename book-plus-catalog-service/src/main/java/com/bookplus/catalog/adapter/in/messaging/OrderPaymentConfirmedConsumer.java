package com.bookplus.catalog.adapter.in.messaging;

import com.bookplus.catalog.adapter.out.persistence.entity.UserPurchaseEntity;
import com.bookplus.catalog.adapter.out.persistence.repository.UserPurchaseJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Proyecta las compras confirmadas en la tabla user_purchases para dar acceso
 * al PDF completo (biblioteca del usuario). Idempotente: inserta solo si falta.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPaymentConfirmedConsumer {

    private final UserPurchaseJpaRepository purchaseRepo;

    @KafkaListener(topics = "order.payment.confirmed", groupId = "catalog-service",
                   containerFactory = "kafkaListenerContainerFactory")
    @Transactional
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
                UUID id = UUID.fromString(bookId);
                if (!purchaseRepo.existsByUserIdAndBookId(userId, id)) {
                    purchaseRepo.save(UserPurchaseEntity.builder()
                            .userId(userId).bookId(id).purchasedAt(Instant.now()).build());
                    log.info("Registered purchase: user={} book={}", userId, bookId);
                }
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
