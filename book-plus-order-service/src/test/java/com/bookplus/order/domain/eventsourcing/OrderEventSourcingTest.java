package com.bookplus.order.domain.eventsourcing;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas del modelo de Event Sourcing + CQRS del pedido:
 *  - los comandos producen eventos y el estado se deriva de ellos,
 *  - el agregado se reconstruye idénticamente reproduciendo su historial (replay),
 *  - la proyección de lectura refleja el mismo resultado.
 */
class OrderEventSourcingTest {

    @Test
    void comandos_producenEventos_yDerivanEstado() {
        EventSourcedOrder order = EventSourcedOrder.place("ORD-1", "cust-9");
        order.addItem("book-A", 2, 1500);   // 2 x 15.00
        order.addItem("book-B", 1, 3000);   // 1 x 30.00
        order.markPaid();

        assertThat(order.status()).isEqualTo(EventSourcedOrder.Status.PAID);
        assertThat(order.totalCents()).isEqualTo(6000);
        assertThat(order.itemCount()).isEqualTo(3);
        // 4 eventos: placed + 2 itemAdded + paid
        assertThat(order.version()).isEqualTo(4);
    }

    @Test
    void replay_reconstruyeElMismoEstado() {
        EventStore store = new EventStore.InMemory();

        EventSourcedOrder order = EventSourcedOrder.place("ORD-2", "cust-1");
        order.addItem("book-A", 3, 1000);
        order.markPaid();
        store.append("ORD-2", order.pullUncommittedEvents());

        // Reconstrucción SOLO desde los eventos guardados.
        EventSourcedOrder rebuilt = EventSourcedOrder.rehydrate(store.load("ORD-2"));

        assertThat(rebuilt.status()).isEqualTo(EventSourcedOrder.Status.PAID);
        assertThat(rebuilt.totalCents()).isEqualTo(3000);
        assertThat(rebuilt.itemCount()).isEqualTo(3);
        assertThat(rebuilt.version()).isEqualTo(order.version());
        assertThat(rebuilt.customerId()).isEqualTo("cust-1");
    }

    @Test
    void proyeccionDeLectura_reflejaLosEventos() {
        OrderSummaryProjection projection = new OrderSummaryProjection();

        EventSourcedOrder order = EventSourcedOrder.place("ORD-3", "cust-7");
        order.addItem("book-Z", 4, 2500);
        order.markPaid();
        order.pullUncommittedEvents().forEach(projection::on);

        OrderSummaryProjection.OrderSummary summary = projection.find("ORD-3").orElseThrow();
        assertThat(summary.status()).isEqualTo("PAID");
        assertThat(summary.totalCents()).isEqualTo(10000);
        assertThat(summary.itemCount()).isEqualTo(4);
        assertThat(summary.customerId()).isEqualTo("cust-7");
    }

    @Test
    void invariante_noSePagaUnPedidoSinLineas() {
        EventSourcedOrder order = EventSourcedOrder.place("ORD-4", "cust-2");
        assertThatThrownBy(order::markPaid).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void invariante_noSeAnadenLineasTrasPagar() {
        EventSourcedOrder order = EventSourcedOrder.place("ORD-5", "cust-3");
        order.addItem("book-A", 1, 1000);
        order.markPaid();
        assertThatThrownBy(() -> order.addItem("book-B", 1, 1000))
                .isInstanceOf(IllegalStateException.class);
    }
}
