package com.bookplus.order.domain.port.out;

import com.bookplus.order.domain.model.Order;
import com.bookplus.order.domain.model.OrderStatus;

import java.util.List;
import java.util.Optional;

public interface LoadOrderPort {
    Optional<Order> findById(String orderId);
    List<Order>     findByUserId(String userId, int page, int size);
    long            countByUserId(String userId);

    /** Admin: todos los pedidos, o filtrados por estado si status != null. */
    List<Order>     findAll(OrderStatus status, int page, int size);
    long            countAll(OrderStatus status);

    /** Cola de fulfillment: pedidos físicos en CONFIRMED o SHIPPED. */
    List<Order>     findShipmentsQueue();

    /** Todos los pedidos en un estado dado (uso interno, p. ej. scheduler). */
    List<Order>     findByStatus(OrderStatus status);
}
