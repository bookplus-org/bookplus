package com.bookplus.order.domain.eventsourcing;

import java.time.Instant;

/**
 * Eventos de dominio del agregado Pedido para el modelo de Event Sourcing.
 *
 * En Event Sourcing el estado NO se guarda como una fila mutable, sino como la
 * secuencia inmutable de hechos que le ocurrieron. El estado actual se obtiene
 * "reproduciendo" (folding) estos eventos desde el principio. Esto da un
 * historial de auditoría perfecto y la capacidad de reconstruir el estado a
 * cualquier punto del tiempo.
 *
 * Interfaz sellada (sealed): el compilador conoce todas las variantes, lo que
 * permite un {@code switch} exhaustivo al aplicarlas.
 */
public sealed interface OrderEvent {

    String orderId();

    Instant occurredAt();

    /** El pedido fue creado. */
    record OrderPlaced(String orderId, String customerId, Instant occurredAt) implements OrderEvent {}

    /** Se añadió una línea al pedido. */
    record ItemAdded(String orderId, String bookId, int quantity, long unitPriceCents, Instant occurredAt)
            implements OrderEvent {}

    /** El pedido fue pagado. */
    record OrderPaid(String orderId, Instant occurredAt) implements OrderEvent {}

    /** El pedido fue cancelado. */
    record OrderCancelled(String orderId, String reason, Instant occurredAt) implements OrderEvent {}
}
