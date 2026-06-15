package com.bookplus.payment.adapter.out.messaging;

import com.bookplus.payment.domain.event.*;
import com.bookplus.payment.domain.model.DomainEvent;
import com.bookplus.payment.domain.port.out.DomainEventPublisherPort;
import com.bookplus.payment.shared.annotation.PersistenceAdapter;
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

    private static final Map<Class<?>, String> TOPICS = Map.of(
            PaymentInitiatedEvent.class,  "payment.initiated",
            PaymentCompletedEvent.class,  "payment.confirmed",
            PaymentFailedEvent.class,     "payment.failed",
            RefundInitiatedEvent.class,   "payment.refunded"
    );

    @Override
    public void publishAll(List<DomainEvent> events) {
        for (DomainEvent event : events) {
            String topic = TOPICS.get(event.getClass());
            if (topic == null) {
                log.warn("No topic mapping for {}", event.getClass().getSimpleName());
                continue;
            }
            try {
                String payload = objectMapper.writeValueAsString(event);
                kafkaTemplate.send(topic, event.aggregateId(), payload)
                        .whenComplete((r, ex) -> {
                            if (ex != null)
                                log.error("Failed sending {} to {}: {}",
                                        event.getClass().getSimpleName(), topic, ex.getMessage());
                            else
                                log.debug("Published {} to {} offset {}",
                                        event.getClass().getSimpleName(), topic,
                                        r.getRecordMetadata().offset());
                        });
            } catch (Exception ex) {
                throw new RuntimeException("Failed to publish " + event.getClass().getSimpleName(), ex);
            }
        }
    }
}
