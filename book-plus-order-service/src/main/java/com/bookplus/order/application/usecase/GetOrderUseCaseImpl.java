package com.bookplus.order.application.usecase;

import com.bookplus.order.domain.exception.OrderNotFoundException;
import com.bookplus.order.domain.model.Order;
import com.bookplus.order.domain.port.in.GetOrderUseCase;
import com.bookplus.order.domain.port.out.LoadOrderPort;
import com.bookplus.order.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;

import java.util.List;

@UseCase @RequiredArgsConstructor
public class GetOrderUseCaseImpl implements GetOrderUseCase {

    private final LoadOrderPort loadOrderPort;

    @Override
    public Order getById(String orderId) {
        return loadOrderPort.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Override
    public List<Order> getByUserId(String userId, int page, int size) {
        return loadOrderPort.findByUserId(userId, page, size);
    }

    @Override
    public long countByUserId(String userId) {
        return loadOrderPort.countByUserId(userId);
    }

    @Override
    public List<Order> getAll(com.bookplus.order.domain.model.OrderStatus status, int page, int size) {
        return loadOrderPort.findAll(status, page, size);
    }

    @Override
    public long countAll(com.bookplus.order.domain.model.OrderStatus status) {
        return loadOrderPort.countAll(status);
    }

    @Override
    public List<Order> getShipmentsQueue(String requesterId, boolean isAdmin) {
        List<Order> all = loadOrderPort.findShipmentsQueue();
        if (isAdmin) {
            return all;
        }
        // Repartidor: ve los pedidos sin asignar (disponibles) y los suyos.
        return all.stream()
                .filter(o -> o.getAssignedCourier() == null
                        || o.getAssignedCourier().equals(requesterId))
                .toList();
    }
}
