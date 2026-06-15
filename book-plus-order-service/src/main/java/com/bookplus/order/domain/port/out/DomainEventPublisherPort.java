package com.bookplus.order.domain.port.out;

import com.bookplus.order.domain.model.DomainEvent;

import java.util.List;

public interface DomainEventPublisherPort {
    void publishAll(List<DomainEvent> events);
}
