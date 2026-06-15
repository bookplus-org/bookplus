package com.bookplus.catalog.domain.port.out;

import com.bookplus.catalog.domain.event.DomainEvent;
import java.util.List;

public interface DomainEventPublisherPort {
    void publish(DomainEvent event);
    void publishAll(List<DomainEvent> events);
}
