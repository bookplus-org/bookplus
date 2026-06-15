package com.bookplus.order.domain.model;

import com.bookplus.order.domain.event.*;
import com.bookplus.order.domain.exception.*;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Pruebas del agregado Order: máquina de estados de la saga de compra
 * (PENDING → PROCESSING → CONFIRMED → SHIPPED → DELIVERED) y de las acciones
 * de compensación (cancelación, reembolso con/sin reposición de stock).
 */
@DisplayName("Order Aggregate — saga de compra")
class OrderTest {

    private static final String USER_ID  = "user-abc";
    private static final String EMAIL    = "buyer@example.com";
    private static final String CART_ID  = "cart-xyz";

    private Money money(String amount) { return Money.of(new BigDecimal(amount), "USD"); }

    private ShippingAddress address() {
        return new ShippingAddress("John Doe", "123 Main St", "Springfield", "IL", "62701", "USA");
    }

    private List<OrderItem> singleItem() {
        return List.of(OrderItem.create("book-1", "ISBN-001", "Clean Code", null, money("29.99"), 2));
    }

    /** Pedido físico recién creado, con los eventos de creación ya consumidos. */
    private Order newOrder() {
        Order o = Order.create(USER_ID, EMAIL, CART_ID, singleItem(), money("59.98"),
                address(), "CARD", "PHYSICAL", null, BigDecimal.ZERO);
        o.pullDomainEvents();
        return o;
    }

    private Order confirmedOrder() {
        Order o = newOrder();
        o.startPaymentProcessing("PAY-001");
        o.confirmPayment();
        o.pullDomainEvents();
        return o;
    }

    // ── create() ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("create() arranca en PENDING_PAYMENT y emite OrderCreatedEvent con el email")
    void create_shouldSetStatusAndPublishEvent() {
        Order order = Order.create(USER_ID, EMAIL, CART_ID, singleItem(), money("59.98"),
                address(), "CARD", "PHYSICAL", null, BigDecimal.ZERO);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(order.getUserId()).isEqualTo(USER_ID);
        assertThat(order.getUserEmail()).isEqualTo(EMAIL);

        List<DomainEvent> events = order.pullDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(OrderCreatedEvent.class);
        assertThat(((OrderCreatedEvent) events.get(0)).getRecipientEmail()).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("create() físico genera un código de entrega de 6 dígitos")
    void create_physical_generatesDeliveryCode() {
        Order order = newOrder();
        assertThat(order.getDeliveryCode()).isNotNull().hasSize(6).containsOnlyDigits();
    }

    @Test
    @DisplayName("create() digital NO genera código de entrega")
    void create_digital_noDeliveryCode() {
        Order order = Order.create(USER_ID, EMAIL, CART_ID, singleItem(), money("59.98"),
                address(), "CARD", "DIGITAL", null, BigDecimal.ZERO);
        assertThat(order.getDeliveryCode()).isNull();
    }

    @Test
    @DisplayName("create() lanza DomainException si no hay ítems")
    void create_emptyItems_shouldThrow() {
        assertThatThrownBy(() -> Order.create(USER_ID, EMAIL, CART_ID, List.of(), money("0"),
                address(), "CARD", "PHYSICAL", null, BigDecimal.ZERO))
                .isInstanceOf(DomainException.class);
    }

    // ── Camino feliz de la saga ──────────────────────────────────────────

    @Test
    @DisplayName("startPaymentProcessing() pasa a PAYMENT_PROCESSING y guarda el paymentId")
    void startPaymentProcessing_success() {
        Order order = newOrder();
        order.startPaymentProcessing("PAY-001");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_PROCESSING);
        assertThat(order.getPaymentId()).isEqualTo("PAY-001");
        assertThat(order.pullDomainEvents()).anyMatch(e -> e instanceof OrderStatusChangedEvent);
    }

    @Test
    @DisplayName("confirmPayment() pasa a CONFIRMED y emite OrderPaymentConfirmedEvent (descuento de stock)")
    void confirmPayment_emitsStockDeductionEvent() {
        Order order = newOrder();
        order.startPaymentProcessing("PAY-001");
        order.pullDomainEvents();

        order.confirmPayment();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.pullDomainEvents()).anyMatch(e -> e instanceof OrderPaymentConfirmedEvent);
    }

    @Test
    @DisplayName("Camino feliz completo: PENDING → PROCESSING → CONFIRMED → SHIPPED → DELIVERED")
    void fullLifecycle_success() {
        Order order = newOrder();
        order.startPaymentProcessing("PAY-001");
        order.confirmPayment();
        order.ship("Olva Courier", "OLV-123");
        order.deliver("Recibido por el cliente");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(order.getStatus().isTerminal()).isTrue();
        assertThat(order.getCarrier()).isEqualTo("Olva Courier");
        assertThat(order.getReceivedBy()).isEqualTo("Recibido por el cliente");
    }

    // ── Compensación: cancelación ────────────────────────────────────────

    @Test
    @DisplayName("cancel() desde PENDING_PAYMENT emite OrderCancelledEvent (libera reserva)")
    void cancel_fromPending_emitsCompensation() {
        Order order = newOrder();
        order.cancel("Cambié de opinión");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.pullDomainEvents()).anyMatch(e -> e instanceof OrderCancelledEvent);
    }

    @Test
    @DisplayName("cancel() desde CONFIRMED lanza OrderNotCancellableException")
    void cancel_fromConfirmed_shouldThrow() {
        Order order = confirmedOrder();
        assertThatThrownBy(() -> order.cancel("Demasiado tarde"))
                .isInstanceOf(OrderNotCancellableException.class);
    }

    // ── Compensación: reembolso ──────────────────────────────────────────

    @Test
    @DisplayName("refund(restock=true) desde DELIVERED pasa a REFUNDED y emite OrderRefundedEvent")
    void refund_withRestock_emitsRefundedEvent() {
        Order order = confirmedOrder();
        order.ship("Olva Courier", "OLV-123");
        order.deliver("Cliente");
        order.pullDomainEvents();

        order.refund("Producto defectuoso", true);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDED);
        List<DomainEvent> events = order.pullDomainEvents();
        assertThat(events).anyMatch(e -> e instanceof OrderRefundedEvent);
        assertThat(events).anyMatch(e -> e instanceof OrderStatusChangedEvent);
    }

    @Test
    @DisplayName("refund(restock=false) NO emite OrderRefundedEvent (no repone stock)")
    void refund_withoutRestock_noRefundedEvent() {
        Order order = confirmedOrder();

        order.refund("Devolución de dinero, producto no revendible", false);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDED);
        assertThat(order.pullDomainEvents()).noneMatch(e -> e instanceof OrderRefundedEvent);
    }

    @Test
    @DisplayName("refund() desde PENDING_PAYMENT lanza DomainException (no está pagado)")
    void refund_fromPending_shouldThrow() {
        Order order = newOrder();
        assertThatThrownBy(() -> order.refund("Aún no paga", false))
                .isInstanceOf(DomainException.class);
    }

    // ── Transiciones inválidas ───────────────────────────────────────────

    @Test
    @DisplayName("ship() desde PENDING_PAYMENT lanza InvalidOrderTransitionException")
    void ship_invalidTransition_shouldThrow() {
        Order order = newOrder();
        assertThatThrownBy(() -> order.ship("Olva", "X1"))
                .isInstanceOf(InvalidOrderTransitionException.class);
    }

    @Test
    @DisplayName("deliver() desde CONFIRMED lanza InvalidOrderTransitionException")
    void deliver_fromConfirmed_shouldThrow() {
        Order order = confirmedOrder();
        assertThatThrownBy(() -> order.deliver("X"))
                .isInstanceOf(InvalidOrderTransitionException.class);
    }
}
