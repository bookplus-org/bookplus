package com.bookplus.order.adapter.out.persistence;

import com.bookplus.order.adapter.in.web.sse.OrderChangedAppEvent;
import com.bookplus.order.adapter.out.persistence.repository.OrderJpaRepository;
import com.bookplus.order.domain.model.Order;
import com.bookplus.order.domain.port.out.*;
import com.bookplus.order.shared.annotation.PersistenceAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@PersistenceAdapter
@RequiredArgsConstructor
public class OrderPersistenceAdapter implements LoadOrderPort, SaveOrderPort {

    private final OrderJpaRepository      repository;
    private final OrderPersistenceMapper  mapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Optional<Order> findById(String orderId) {
        return repository.findById(UUID.fromString(orderId))
                .map(mapper::toDomain);
    }

    @Override
    public List<Order> findByUserId(String userId, int page, int size) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public long countByUserId(String userId) {
        return repository.countByUserId(userId);
    }

    @Override
    public List<Order> findAll(com.bookplus.order.domain.model.OrderStatus status, int page, int size) {
        var pageable = PageRequest.of(page, size);
        var entities = status == null
                ? repository.findAllByOrderByCreatedAtDesc(pageable)
                : repository.findByStatusOrderByCreatedAtDesc(status, pageable);
        return entities.stream().map(mapper::toDomain).toList();
    }

    @Override
    public long countAll(com.bookplus.order.domain.model.OrderStatus status) {
        return status == null ? repository.count() : repository.countByStatus(status);
    }

    @Override
    public List<Order> findShipmentsQueue() {
        return repository.findByDeliveryTypeAndStatusInOrderByCreatedAtAsc(
                        "PHYSICAL",
                        List.of(com.bookplus.order.domain.model.OrderStatus.CONFIRMED,
                                com.bookplus.order.domain.model.OrderStatus.SHIPPED))
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Order> findByStatus(com.bookplus.order.domain.model.OrderStatus status) {
        return repository.findByStatus(status).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Order save(Order order) {
        Order saved = mapper.toDomain(repository.save(mapper.toEntity(order)));
        // Notifica a las conexiones SSE del dueño para refresco en tiempo real.
        eventPublisher.publishEvent(new OrderChangedAppEvent(
                saved.getUserId(), saved.getId().toString(), saved.getStatus().name()));
        return saved;
    }
}
