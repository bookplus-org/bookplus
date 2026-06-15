package com.bookplus.auth.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Marker interface para todos los Domain Events.
 * Los events son inmutables y describen algo que ocurrió en el dominio.
 */
public interface DomainEvent {
    UUID eventId();
    String eventType();
    Instant occurredOn();
}
