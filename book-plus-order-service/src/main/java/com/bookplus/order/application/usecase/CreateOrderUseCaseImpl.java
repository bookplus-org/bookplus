package com.bookplus.order.application.usecase;

import com.bookplus.order.domain.model.*;
import com.bookplus.order.domain.port.in.CreateOrderUseCase;
import com.bookplus.order.domain.port.out.*;
import com.bookplus.order.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Creates an order and writes domain events to the outbox table
 * IN THE SAME @Transactional boundary.
 *
 * Either both the Order row and the outbox rows are committed,
 * or neither is — eliminating the "save succeeded but event was lost" race.
 */
@UseCase @RequiredArgsConstructor @Slf4j
public class CreateOrderUseCaseImpl implements CreateOrderUseCase {

    private final SaveOrderPort            saveOrderPort;
    private final OutboxEventPublisherPort outboxPublisher;  // writes to DB, not Kafka directly

    @Override
    @Transactional
    public Order createOrder(CreateOrderCommand cmd) {
        List<OrderItem> items = cmd.items().stream()
                .map(dto -> OrderItem.create(
                        dto.bookId(), dto.isbn(), dto.title(), dto.imageUrl(),
                        Money.of(dto.unitPrice(), dto.currency()), dto.quantity()))
                .toList();

        Money total = Money.of(cmd.total(), cmd.currency());
        Order order = Order.create(cmd.userId(), cmd.userEmail(), cmd.cartId(), items, total,
                cmd.shippingAddress(), cmd.paymentMethod(), cmd.deliveryType(),
                cmd.couponCode(), cmd.discountAmount());
        Order saved = saveOrderPort.save(order);

        // Atomically write events to outbox — OutboxRelay forwards to Kafka asynchronously
        outboxPublisher.saveAll("Order", order.pullDomainEvents());

        log.info("Order {} created for user {} — {} items, total {}{}",
                saved.getId(), cmd.userId(), items.size(), total.amount(), total.currency());
        return saved;
    }
}
