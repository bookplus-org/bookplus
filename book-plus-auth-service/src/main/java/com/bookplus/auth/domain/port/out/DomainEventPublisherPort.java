package com.bookplus.auth.domain.port.out;

import com.bookplus.auth.domain.event.DomainEvent;

import java.util.List;

/** Puerto de salida — publicar domain events hacia Kafka. */
public interface DomainEventPublisherPort {
    void publish(DomainEvent event);
    void publishAll(List<DomainEvent> events);
}
