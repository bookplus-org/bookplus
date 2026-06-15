package com.bookplus.payment.domain.port.out;

import com.bookplus.payment.domain.model.DomainEvent;
import java.util.List;

public interface DomainEventPublisherPort {
    void publishAll(List<DomainEvent> events);
}
