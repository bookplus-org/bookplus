package com.bookplus.inventory.domain.model;

import com.bookplus.inventory.domain.event.*;
import com.bookplus.inventory.domain.exception.DomainException;
import com.bookplus.inventory.domain.exception.InsufficientStockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Stock Aggregate")
class StockTest {

    private BookId bookId;

    @BeforeEach
    void setUp() {
        bookId = BookId.generate();
    }

    // ── create() ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("create() inicializa con cantidad correcta y emite StockCreatedEvent")
    void create_shouldSetFieldsAndPublishEvent() {
        Stock stock = Stock.create(bookId, 100, 10);

        assertThat(stock.getQuantityTotal()).isEqualTo(100);
        assertThat(stock.getQuantityAvailable()).isEqualTo(100);
        assertThat(stock.getQuantityReserved()).isZero();
        assertThat(stock.isOutOfStock()).isFalse();

        List<DomainEvent> events = stock.pullDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(StockCreatedEvent.class);
    }

    @Test
    @DisplayName("create() con cantidad negativa lanza DomainException")
    void create_negativeQuantity_shouldThrow() {
        assertThatThrownBy(() -> Stock.create(bookId, -1, 5))
                .isInstanceOf(DomainException.class);
    }

    // ── addStock() ────────────────────────────────────────────────────────

    @Test
    @DisplayName("addStock() incrementa available y total, emite StockUpdatedEvent")
    void addStock_shouldIncreaseQuantity() {
        Stock stock = Stock.create(bookId, 50, 10);
        stock.pullDomainEvents();

        StockMovement movement = stock.addStock(25, "PO-001", "Purchase order");

        assertThat(stock.getQuantityTotal()).isEqualTo(75);
        assertThat(stock.getQuantityAvailable()).isEqualTo(75);
        assertThat(movement.getType()).isEqualTo(MovementType.IN);
        assertThat(movement.getQuantity()).isEqualTo(25);

        List<DomainEvent> events = stock.pullDomainEvents();
        assertThat(events).anyMatch(e -> e instanceof StockUpdatedEvent);
    }

    // ── reserve() ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("reserve() reduce available y aumenta reserved")
    void reserve_shouldMoveQuantityFromAvailableToReserved() {
        Stock stock = Stock.create(bookId, 100, 5);
        stock.pullDomainEvents();

        StockMovement movement = stock.reserve(10, "ORD-001");

        assertThat(stock.getQuantityAvailable()).isEqualTo(90);
        assertThat(stock.getQuantityReserved()).isEqualTo(10);
        assertThat(stock.getQuantityTotal()).isEqualTo(100); // no cambia
        assertThat(movement.getType()).isEqualTo(MovementType.RESERVED);

        List<DomainEvent> events = stock.pullDomainEvents();
        assertThat(events).anyMatch(e -> e instanceof StockUpdatedEvent);
    }

    @Test
    @DisplayName("reserve() lanza InsufficientStockException si no hay stock suficiente")
    void reserve_insufficient_shouldThrow() {
        Stock stock = Stock.create(bookId, 5, 2);
        assertThatThrownBy(() -> stock.reserve(10, "ORD-001"))
                .isInstanceOf(InsufficientStockException.class);
    }

    // ── confirmReservation() ──────────────────────────────────────────────

    @Test
    @DisplayName("confirmReservation() descuenta del total y reserved")
    void confirmReservation_shouldReduceTotalAndReserved() {
        Stock stock = Stock.create(bookId, 100, 5);
        stock.reserve(20, "ORD-001");
        stock.pullDomainEvents();

        StockMovement movement = stock.confirmReservation(20, "ORD-001");

        assertThat(stock.getQuantityTotal()).isEqualTo(80);
        assertThat(stock.getQuantityAvailable()).isEqualTo(80);
        assertThat(stock.getQuantityReserved()).isZero();
        assertThat(movement.getType()).isEqualTo(MovementType.OUT);
    }

    // ── releaseReservation() ──────────────────────────────────────────────

    @Test
    @DisplayName("releaseReservation() devuelve el stock a available")
    void releaseReservation_shouldRestoreAvailable() {
        Stock stock = Stock.create(bookId, 100, 5);
        stock.reserve(15, "ORD-001");
        stock.pullDomainEvents();

        StockMovement movement = stock.releaseReservation(15, "ORD-001", "Order cancelled");

        assertThat(stock.getQuantityAvailable()).isEqualTo(100);
        assertThat(stock.getQuantityReserved()).isZero();
        assertThat(movement.getType()).isEqualTo(MovementType.UNRESERVED);
    }

    // ── adjust() ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("adjust() recalcula available manteniendo reserved intacto")
    void adjust_shouldRecalculateAvailable() {
        Stock stock = Stock.create(bookId, 100, 5);
        stock.reserve(10, "ORD-001");
        stock.pullDomainEvents();

        stock.adjust(80, "Physical count");

        assertThat(stock.getQuantityTotal()).isEqualTo(80);
        assertThat(stock.getQuantityReserved()).isEqualTo(10);
        assertThat(stock.getQuantityAvailable()).isEqualTo(70);
    }

    // ── LowStock alert ────────────────────────────────────────────────────

    @Test
    @DisplayName("reserve() emite LowStockAlertEvent cuando available cae por debajo del umbral")
    void reserve_shouldEmitLowStockAlert() {
        Stock stock = Stock.create(bookId, 12, 10); // threshold = 10
        stock.pullDomainEvents();

        stock.reserve(3, "ORD-001"); // available = 9 < 10

        List<DomainEvent> events = stock.pullDomainEvents();
        assertThat(events).anyMatch(e -> e instanceof LowStockAlertEvent);
    }

    // ── Invariantes ───────────────────────────────────────────────────────

    @Test
    @DisplayName("isAvailable() devuelve true solo si hay suficiente stock")
    void isAvailable_shouldReflectCorrectly() {
        Stock stock = Stock.create(bookId, 5, 2);
        assertThat(stock.isAvailable(5)).isTrue();
        assertThat(stock.isAvailable(6)).isFalse();
    }
}
