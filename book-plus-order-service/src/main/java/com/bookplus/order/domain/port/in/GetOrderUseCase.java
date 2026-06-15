package com.bookplus.order.domain.port.in;

import com.bookplus.order.domain.model.*;

import java.util.List;

public interface GetOrderUseCase {
    Order getById(String orderId);
    List<Order> getByUserId(String userId, int page, int size);
    long countByUserId(String userId);

    /** Admin: todos los pedidos (status null = todos). */
    List<Order> getAll(OrderStatus status, int page, int size);
    long countAll(OrderStatus status);

    /**
     * Cola de envíos (pedidos físicos por enviar/entregar).
     * Admin ve todo; un repartidor ve los no asignados + los suyos.
     */
    List<Order> getShipmentsQueue(String requesterId, boolean isAdmin);
}
