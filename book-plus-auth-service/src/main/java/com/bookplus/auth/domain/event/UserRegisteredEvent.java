package com.bookplus.auth.domain.event;

import com.bookplus.auth.domain.model.Email;
import com.bookplus.auth.domain.model.UserId;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain Event — disparado cuando un nuevo usuario se registra exitosamente.
 * Kafka topic: auth.user.registered
 */
public record UserRegisteredEvent(
        UUID eventId,
        String eventType,
        Instant occurredOn,
        UserId userId,
        Email email,
        String username
) implements DomainEvent {

    public UserRegisteredEvent(UserId userId, Email email, String username) {
        this(UUID.randomUUID(), "USER_REGISTERED", Instant.now(), userId, email, username);
    }
}
