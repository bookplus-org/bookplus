package com.bookplus.order.application.usecase;

import com.bookplus.order.domain.exception.*;
import com.bookplus.order.domain.model.Order;
import com.bookplus.order.domain.port.in.CancelOrderUseCase;
import com.bookplus.order.domain.port.out.*;
import com.bookplus.order.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase @RequiredArgsConstructor @Slf4j
public class CancelOrderUseCaseImpl implements CancelOrderUseCase {

    private final LoadOrderPort            loadOrderPort;
    private final SaveOrderPort            saveOrderPort;
    private final OutboxEventPublisherPort outboxPublisher;

    @Override
    @org.springframework.transaction.annotation.Transactional
    public Order cancel(String orderId, String requestingUserId, String reason) {
        Order order = loadOrderPort.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!order.getUserId().equals(requestingUserId))
            throw new DomainException("You are not allowed to cancel order " + orderId);

        // El cliente solo puede cancelar antes de que el pago se confirme.
        var status = order.getStatus();
        if (status != com.bookplus.order.domain.model.OrderStatus.PENDING_PAYMENT
                && status != com.bookplus.order.domain.model.OrderStatus.PAYMENT_PROCESSING) {
            throw new DomainException("No puedes cancelar un pedido cuyo pago ya fue confirmado");
        }

        order.cancel(reason);
        Order saved = saveOrderPort.save(order);
        outboxPublisher.saveAll("Order", order.pullDomainEvents());
        return saved;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public Order cancelAsAdmin(String orderId, String reason) {
        Order order = loadOrderPort.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        order.cancel(reason);
        Order saved = saveOrderPort.save(order);
        outboxPublisher.saveAll("Order", order.pullDomainEvents());
        return saved;
    }
}
