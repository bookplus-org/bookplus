package com.bookplus.order.domain.eventsourcing;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Proyección de lectura (modelo de LECTURA del patrón CQRS).
 *
 * CQRS separa el modelo de escritura ({@link EventSourcedOrder}, optimizado para
 * validar invariantes) del de lectura (esta proyección, optimizada para consultar).
 * La proyección "escucha" los mismos eventos y mantiene una vista desnormalizada
 * y lista para servir, sin tener que reproducir todo el historial en cada consulta.
 */
public final class OrderSummaryProjection {

    /** Vista de lectura desnormalizada de un pedido. */
    public record OrderSummary(String orderId, String customerId, String status,
                               long totalCents, int itemCount) {}

    private final Map<String, OrderSummary> view = new ConcurrentHashMap<>();

    /** Actualiza la vista aplicando un evento (idempotente por reconstrucción de estado). */
    public void on(OrderEvent event) {
        String id = event.orderId();
        OrderSummary current = view.get(id);
        switch (event) {
            case OrderEvent.OrderPlaced e ->
                    view.put(id, new OrderSummary(id, e.customerId(), "PLACED", 0, 0));
            case OrderEvent.ItemAdded e -> {
                OrderSummary s = require(current, id);
                view.put(id, new OrderSummary(id, s.customerId(), s.status(),
                        s.totalCents() + (long) e.quantity() * e.unitPriceCents(),
                        s.itemCount() + e.quantity()));
            }
            case OrderEvent.OrderPaid e -> {
                OrderSummary s = require(current, id);
                view.put(id, new OrderSummary(id, s.customerId(), "PAID", s.totalCents(), s.itemCount()));
            }
            case OrderEvent.OrderCancelled e -> {
                OrderSummary s = require(current, id);
                view.put(id, new OrderSummary(id, s.customerId(), "CANCELLED", s.totalCents(), s.itemCount()));
            }
        }
    }

    public Optional<OrderSummary> find(String orderId) {
        return Optional.ofNullable(view.get(orderId));
    }

    private static OrderSummary require(OrderSummary current, String id) {
        if (current == null) {
            throw new IllegalStateException("evento para un pedido no proyectado: " + id);
        }
        return current;
    }
}
