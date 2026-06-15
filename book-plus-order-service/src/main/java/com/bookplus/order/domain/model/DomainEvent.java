package com.bookplus.order.domain.model;

import java.time.Instant;

public interface DomainEvent {
    String  aggregateId();
    Instant occurredOn();
}
