package com.bookplus.order.domain.port.out;

import com.bookplus.order.domain.model.DomainEvent;
import java.util.List;

/**
 * Saves domain events to the outbox table IN THE SAME transaction
 * as the aggregate save.  The OutboxRelay is responsible for reading
 * these rows and forwarding them to Kafka asynchronously.
 *
 * This replaces the direct DomainEventPublisherPort in use cases
 * where transactional safety is required.
 */
public interface OutboxEventPublisherPort {
    void saveAll(String aggregateType, List<DomainEvent> events);
}
