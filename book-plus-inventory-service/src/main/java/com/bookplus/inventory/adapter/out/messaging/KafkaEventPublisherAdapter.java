package com.bookplus.inventory.adapter.out.messaging;

import com.bookplus.inventory.domain.event.*;
import com.bookplus.inventory.domain.port.out.DomainEventPublisherPort;
import com.bookplus.inventory.shared.annotation.PersistenceAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.Map;

@PersistenceAdapter
@RequiredArgsConstructor
@Slf4j
public class KafkaEventPublisherAdapter implements DomainEventPublisherPort {

    private static final Map<Class<?>, String> TOPIC_MAP = Map.of(
            StockCreatedEvent.class, "inventory.stock.created",
            StockUpdatedEvent.class, "inventory.stock.updated",
            LowStockAlertEvent.class, "inventory.stock.low-alert",
            StockReservedEvent.class, "inventory.stock.reserved",
            StockReleasedEvent.class, "inventory.stock.released"
    );

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publish(DomainEvent event) {
        String topic = TOPIC_MAP.get(event.getClass());
        if (topic == null) {
            log.warn("No topic mapped for event: {}", event.getClass().getSimpleName());
            return;
        }
        kafkaTemplate.send(topic, event.eventId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish {} to '{}': {}", event.getClass().getSimpleName(), topic, ex.getMessage());
                    } else {
                        log.debug("Published {} to '{}' partition={} offset={}",
                                event.getClass().getSimpleName(), topic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    @Override
    public void publishAll(List<DomainEvent> events) {
        events.forEach(this::publish);
    }
}
