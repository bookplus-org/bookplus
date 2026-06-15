package com.bookplus.inventory.domain.port.out;

import com.bookplus.inventory.domain.event.DomainEvent;

import java.util.List;

public interface DomainEventPublisherPort {
    void publish(DomainEvent event);
    void publishAll(List<DomainEvent> events);
}
