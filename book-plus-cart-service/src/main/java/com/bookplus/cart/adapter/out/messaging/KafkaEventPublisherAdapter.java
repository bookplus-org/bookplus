package com.bookplus.cart.adapter.out.messaging;

import com.bookplus.cart.domain.event.*;
import com.bookplus.cart.domain.port.out.DomainEventPublisherPort;
import com.bookplus.cart.shared.annotation.PersistenceAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.Map;

@PersistenceAdapter
@RequiredArgsConstructor
@Slf4j
public class KafkaEventPublisherAdapter implements DomainEventPublisherPort {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper                  objectMapper;

    /** Topic mapping — cartId used as Kafka partition key for ordering guarantees */
    private static final Map<Class<?>, String> TOPIC_MAP = Map.of(
            CartItemAddedEvent.class,   "cart.item.added",
            CartItemRemovedEvent.class, "cart.item.removed",
            CartClearedEvent.class,     "cart.cleared",
            CartCheckedOutEvent.class,  "cart.checked-out"
    );

    @Override
    public void publish(DomainEvent event) {
        publishAll(List.of(event));
    }

    @Override
    public void publishAll(List<DomainEvent> events) {
        for (DomainEvent event : events) {
            String topic = TOPIC_MAP.get(event.getClass());
            if (topic == null) {
                log.warn("No topic mapping for event type {}", event.getClass().getSimpleName());
                continue;
            }
            try {
                String payload = objectMapper.writeValueAsString(event);
                kafkaTemplate.send(topic, event.aggregateId(), payload)
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.error("Failed to send {} to topic {}: {}",
                                        event.getClass().getSimpleName(), topic, ex.getMessage());
                            } else {
                                log.debug("Published {} to topic {} offset {}",
                                        event.getClass().getSimpleName(), topic,
                                        result.getRecordMetadata().offset());
                            }
                        });
            } catch (Exception ex) {
                log.error("Serialization error for {}: {}", event.getClass().getSimpleName(), ex.getMessage());
                throw new RuntimeException("Failed to publish domain event", ex);
            }
        }
    }
}
