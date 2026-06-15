package com.bookplus.payment.domain.model;

import java.time.Instant;

public interface DomainEvent {
    String  aggregateId();
    Instant occurredOn();
}
