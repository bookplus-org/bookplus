package com.bookplus.auth.adapter.out.kafka;

import com.bookplus.auth.domain.event.DomainEvent;
import com.bookplus.auth.domain.event.UserDeactivatedEvent;
import com.bookplus.auth.domain.event.UserRegisteredEvent;
import com.bookplus.auth.domain.port.out.DomainEventPublisherPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Adaptador Kafka — publica Domain Events a los topics correspondientes.
 *
 * Mapping de eventos a topics:
 *   UserRegisteredEvent  → auth.user.registered
 *   UserDeactivatedEvent → auth.user.deactivated
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventPublisherAdapter implements DomainEventPublisherPort {

    private static final Map<Class<?>, String> EVENT_TOPIC_MAP = Map.of(
            UserRegisteredEvent.class,  "auth.user.registered",
            UserDeactivatedEvent.class, "auth.user.deactivated"
    );

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper                  objectMapper;

    @Override
    public void publish(DomainEvent event) {
        String topic = EVENT_TOPIC_MAP.getOrDefault(event.getClass(), "auth.events.unknown");
        String key   = event.eventId().toString();

        try {
            kafkaTemplate.send(topic, key, event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish event {} to topic {}: {}",
                                    event.eventType(), topic, ex.getMessage());
                        } else {
                            log.debug("Event {} published to topic {} partition {} offset {}",
                                    event.eventType(), topic,
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception e) {
            log.error("Error publishing domain event {}: {}", event.eventType(), e.getMessage(), e);
        }
    }

    @Override
    public void publishAll(List<DomainEvent> events) {
        events.forEach(this::publish);
    }
}
