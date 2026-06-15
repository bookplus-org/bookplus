package com.bookplus.auth.domain.event;

import com.bookplus.auth.domain.model.Email;
import com.bookplus.auth.domain.model.UserId;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain Event — disparado cuando un usuario es desactivado.
 * Kafka topic: auth.user.deactivated
 */
public record UserDeactivatedEvent(
        UUID eventId,
        String eventType,
        Instant occurredOn,
        UserId userId,
        Email email,
        String reason
) implements DomainEvent {

    public UserDeactivatedEvent(UserId userId, Email email, String reason) {
        this(UUID.randomUUID(), "USER_DEACTIVATED", Instant.now(), userId, email, reason);
    }
}
