package com.bookplus.cart.domain.event;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {
    UUID    eventId();
    String  eventType();
    Instant occurredOn();

    /** Clave de partición Kafka. Por defecto el id del evento. */
    default String aggregateId() {
        return eventId().toString();
    }
}
