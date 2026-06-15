package com.bookplus.report.adapter.in.messaging;

import com.bookplus.report.domain.model.OrderEvent;
import com.bookplus.report.domain.port.out.SaveOrderEventPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Ingests order events for reporting purposes.
 * Persists raw events + updates daily_sales aggregation table.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final SaveOrderEventPort saveOrderEventPort;

    @KafkaListener(topics = {"order.created", "order.cancelled", "order.status.changed"},
                   groupId = "report-service-orders",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onOrderEvent(
            @Payload Map<String, Object> payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_KEY)   String key
    ) {
        log.debug("Report ingesting event from topic={} key={}", topic, key);
        try {
            String orderId   = (String) payload.get("orderId");
            String userId    = (String) payload.get("userId");
            String eventType = topicToEventType(topic, payload);

            @SuppressWarnings("unchecked")
            Map<String, Object> totalMap = (Map<String, Object>) payload.getOrDefault("total",
                    Map.of("amount", "0", "currency", "USD"));

            BigDecimal total    = new BigDecimal(totalMap.get("amount").toString());
            String     currency = (String) totalMap.getOrDefault("currency", "USD");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rawItems =
                    (List<Map<String, Object>>) payload.getOrDefault("items", List.of());

            List<OrderEvent.ItemSnapshot> items = rawItems.stream().map(i ->
                    OrderEvent.ItemSnapshot.builder()
                            .bookId((String) i.get("bookId"))
                            .isbn((String) i.getOrDefault("isbn", ""))
                            .title((String) i.getOrDefault("title", ""))
                            .quantity(((Number) i.getOrDefault("quantity", 0)).intValue())
                            .unitPrice(new BigDecimal(i.getOrDefault("unitPrice", "0").toString()))
                            .build()
            ).toList();

            OrderEvent event = OrderEvent.builder()
                    .orderId(orderId)
                    .userId(userId)
                    .eventType(eventType)
                    .total(total)
                    .currency(currency)
                    .items(items)
                    .occurredOn(Instant.now())
                    .build();

            saveOrderEventPort.save(event);

        } catch (Exception ex) {
            log.error("Failed to ingest report event from {}: {}", topic, ex.getMessage(), ex);
        }
    }

    private String topicToEventType(String topic, Map<String, Object> payload) {
        return switch (topic) {
            case "order.created"       -> "ORDER_CREATED";
            case "order.cancelled"     -> "ORDER_CANCELLED";
            case "order.status.changed" -> "ORDER_STATUS_" + payload.getOrDefault("newStatus", "UNKNOWN");
            default                    -> topic.toUpperCase().replace(".", "_");
        };
    }
}
