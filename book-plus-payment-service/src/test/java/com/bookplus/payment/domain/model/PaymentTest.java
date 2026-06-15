package com.bookplus.payment.domain.model;

import com.bookplus.payment.domain.event.*;
import com.bookplus.payment.domain.exception.*;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Payment Aggregate")
class PaymentTest {

    private static final Money  AMOUNT = Money.of(new BigDecimal("59.99"), "USD");
    private static final String ORDER  = "order-001";
    private static final String USER   = "user-abc";

    private Payment newPending() {
        Payment p = Payment.initiate(ORDER, USER, AMOUNT, PaymentMethod.CREDIT_CARD);
        p.pullDomainEvents();
        return p;
    }

    // ── initiate() ────────────────────────────────────────────────────────

    @Test
    @DisplayName("initiate() creates payment in PENDING and emits PaymentInitiatedEvent")
    void initiate_createsAndPublishesEvent() {
        Payment p = Payment.initiate(ORDER, USER, AMOUNT, PaymentMethod.CREDIT_CARD);

        assertThat(p.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(p.getOrderId()).isEqualTo(ORDER);

        List<DomainEvent> events = p.pullDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(PaymentInitiatedEvent.class);
    }

    // ── process() ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("process() transitions PENDING → PROCESSING")
    void process_success() {
        Payment p = newPending();
        p.process();
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
    }

    // ── complete() ────────────────────────────────────────────────────────

    @Test
    @DisplayName("complete() transitions PROCESSING → COMPLETED and emits PaymentCompletedEvent")
    void complete_success() {
        Payment p = newPending();
        p.process();
        p.complete("TXN-9999");

        assertThat(p.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(p.getGatewayTransactionRef()).isEqualTo("TXN-9999");

        List<DomainEvent> events = p.pullDomainEvents();
        assertThat(events).anyMatch(e -> e instanceof PaymentCompletedEvent);
    }

    @Test
    @DisplayName("complete() from PENDING throws InvalidPaymentTransitionException")
    void complete_fromPending_shouldThrow() {
        Payment p = newPending();
        assertThatThrownBy(() -> p.complete("TXN-X"))
                .isInstanceOf(InvalidPaymentTransitionException.class);
    }

    // ── fail() ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("fail() transitions to FAILED and emits PaymentFailedEvent")
    void fail_fromProcessing_success() {
        Payment p = newPending();
        p.process();
        p.fail("Insufficient funds");

        assertThat(p.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(p.getFailureReason()).isEqualTo("Insufficient funds");
        assertThat(p.getStatus().isTerminal()).isTrue();

        List<DomainEvent> events = p.pullDomainEvents();
        assertThat(events).anyMatch(e -> e instanceof PaymentFailedEvent);
    }

    // ── refund() ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("refund() transitions COMPLETED → REFUNDED and emits RefundInitiatedEvent")
    void refund_success() {
        Payment p = newPending();
        p.process();
        p.complete("TXN-001");
        p.pullDomainEvents();

        p.refund("Order cancelled by user");

        assertThat(p.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(p.getStatus().isTerminal()).isTrue();

        List<DomainEvent> events = p.pullDomainEvents();
        assertThat(events).anyMatch(e -> e instanceof RefundInitiatedEvent);
    }

    @Test
    @DisplayName("refund() from PENDING throws InvalidPaymentTransitionException")
    void refund_fromPending_shouldThrow() {
        Payment p = newPending();
        assertThatThrownBy(() -> p.refund("Too early"))
                .isInstanceOf(InvalidPaymentTransitionException.class);
    }

    // ── Terminal state guard ───────────────────────────────────────────────

    @Test
    @DisplayName("Any transition from a terminal state throws InvalidPaymentTransitionException")
    void terminalState_noFurtherTransitions() {
        Payment p = newPending();
        p.fail("Network timeout");
        p.pullDomainEvents();

        assertThatThrownBy(() -> p.process())
                .isInstanceOf(InvalidPaymentTransitionException.class);
    }
}
