package com.bookplus.catalog.adapter.out.messaging;

import com.bookplus.catalog.domain.event.*;
import com.bookplus.catalog.domain.port.out.DomainEventPublisherPort;
import com.bookplus.catalog.shared.annotation.PersistenceAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.Map;

/**
 * Adapter OUT — Publica Domain Events en Kafka.
 *
 * Topics:
 *   catalog.book.created   → BookCreatedEvent
 *   catalog.book.updated   → BookUpdatedEvent
 *   catalog.book.deleted   → BookDeletedEvent
 *   catalog.review.added   → ReviewAddedEvent
 */
@PersistenceAdapter
@RequiredArgsConstructor
@Slf4j
public class KafkaEventPublisherAdapter implements DomainEventPublisherPort {

    private static final Map<Class<?>, String> TOPIC_MAP = Map.of(
            BookCreatedEvent.class,  "catalog.book.created",
            BookUpdatedEvent.class,  "catalog.book.updated",
            BookDeletedEvent.class,  "catalog.book.deleted",
            ReviewAddedEvent.class,  "catalog.review.added"
    );

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publish(DomainEvent event) {
        String topic = TOPIC_MAP.get(event.getClass());
        if (topic == null) {
            log.warn("No Kafka topic mapped for event type: {}", event.getClass().getSimpleName());
            return;
        }
        kafkaTemplate.send(topic, event.eventId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish {} to topic '{}': {}",
                                event.getClass().getSimpleName(), topic, ex.getMessage());
                    } else {
                        log.debug("Published {} to topic '{}' partition={} offset={}",
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
