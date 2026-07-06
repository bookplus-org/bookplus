package com.bookplus.order.domain.eventsourcing;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agregado Pedido en versión Event-Sourced (modelo de ESCRITURA del patrón CQRS).
 *
 * Reglas del patrón:
 *  - Los comandos ({@link #place}, {@link #addItem}, {@link #markPaid}, {@link #cancel})
 *    validan la invariante y, si es válida, EMITEN un evento; nunca mutan el estado
 *    directamente.
 *  - {@link #apply} es la ÚNICA vía de cambio de estado, y es pura (sin efectos):
 *    se usa tanto al emitir un evento nuevo como al reconstruir desde el historial.
 *  - {@link #rehydrate} reconstruye el agregado reproduciendo sus eventos (replay).
 */
public final class EventSourcedOrder {

    public enum Status { PLACED, PAID, CANCELLED }

    private final String id;
    private String customerId;
    private final Map<String, Integer> items = new LinkedHashMap<>();
    private long totalCents;
    private Status status;
    private long version;

    private final List<OrderEvent> uncommitted = new ArrayList<>();

    private EventSourcedOrder(String id) {
        this.id = id;
    }

    // ── Comandos (emiten eventos) ────────────────────────────────────────────

    public static EventSourcedOrder place(String orderId, String customerId) {
        EventSourcedOrder order = new EventSourcedOrder(orderId);
        order.raise(new OrderEvent.OrderPlaced(orderId, customerId, Instant.now()));
        return order;
    }

    public void addItem(String bookId, int quantity, long unitPriceCents) {
        requireStatus(Status.PLACED, "solo se pueden añadir líneas a un pedido en estado PLACED");
        if (quantity <= 0) throw new IllegalArgumentException("quantity debe ser > 0");
        raise(new OrderEvent.ItemAdded(id, bookId, quantity, unitPriceCents, Instant.now()));
    }

    public void markPaid() {
        requireStatus(Status.PLACED, "solo se puede pagar un pedido en estado PLACED");
        if (items.isEmpty()) throw new IllegalStateException("no se puede pagar un pedido sin líneas");
        raise(new OrderEvent.OrderPaid(id, Instant.now()));
    }

    public void cancel(String reason) {
        if (status == Status.PAID) throw new IllegalStateException("un pedido pagado no se cancela, se reembolsa");
        if (status == Status.CANCELLED) return;
        raise(new OrderEvent.OrderCancelled(id, reason, Instant.now()));
    }

    // ── Reconstrucción desde el historial (replay) ───────────────────────────

    public static EventSourcedOrder rehydrate(List<OrderEvent> history) {
        if (history == null || history.isEmpty()) {
            throw new IllegalArgumentException("no se puede reconstruir un pedido sin eventos");
        }
        EventSourcedOrder order = new EventSourcedOrder(history.get(0).orderId());
        for (OrderEvent event : history) {
            order.apply(event);
        }
        return order;
    }

    // ── Núcleo: emitir vs aplicar ────────────────────────────────────────────

    private void raise(OrderEvent event) {
        apply(event);
        uncommitted.add(event);
    }

    /** Única transición de estado. Pura: mismos eventos → mismo estado. */
    private void apply(OrderEvent event) {
        switch (event) {
            case OrderEvent.OrderPlaced e -> {
                this.customerId = e.customerId();
                this.status = Status.PLACED;
            }
            case OrderEvent.ItemAdded e -> {
                this.items.merge(e.bookId(), e.quantity(), Integer::sum);
                this.totalCents += (long) e.quantity() * e.unitPriceCents();
            }
            case OrderEvent.OrderPaid e -> this.status = Status.PAID;
            case OrderEvent.OrderCancelled e -> this.status = Status.CANCELLED;
        }
        this.version++;
    }

    private void requireStatus(Status expected, String message) {
        if (this.status != expected) throw new IllegalStateException(message);
    }

    /** Devuelve y limpia los eventos aún no persistidos (para escribirlos en el event store). */
    public List<OrderEvent> pullUncommittedEvents() {
        List<OrderEvent> copy = List.copyOf(uncommitted);
        uncommitted.clear();
        return copy;
    }

    // ── Getters del estado reconstruido ──────────────────────────────────────

    public String id()               { return id; }
    public String customerId()       { return customerId; }
    public Status status()           { return status; }
    public long totalCents()         { return totalCents; }
    public long version()            { return version; }
    public Map<String, Integer> items() { return Map.copyOf(items); }
    public int itemCount()           { return items.values().stream().mapToInt(Integer::intValue).sum(); }
}
