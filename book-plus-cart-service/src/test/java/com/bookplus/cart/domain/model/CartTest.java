package com.bookplus.cart.domain.model;

import com.bookplus.cart.domain.event.*;
import com.bookplus.cart.domain.exception.DomainException;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Cart Aggregate")
class CartTest {

    private static final String USER_ID = "user-abc";

    private Money price(String amount) { return Money.of(new BigDecimal(amount), "USD"); }
    /** BookId.of() exige un UUID; derivamos uno determinista del texto (mismo texto → mismo id). */
    private BookId book(String id) {
        return BookId.of(UUID.nameUUIDFromBytes(id.getBytes()).toString());
    }

    private Cart newCart() {
        Cart c = Cart.createFor(USER_ID);
        c.pullDomainEvents(); // clear creation event if any
        return c;
    }

    /** Adaptador a la firma actual addItem(bookId, title, imageUrl, isbn, quantity, unitPrice). */
    private void add(Cart cart, BookId bookId, String isbn, String title, Money price, int qty) {
        cart.addItem(bookId, title, null, isbn, qty, price);
    }

    /** getItems() ahora devuelve una Collection; localizamos el ítem por bookId. */
    private CartItem item(Cart cart, BookId bookId) {
        return cart.getItems().stream()
                .filter(i -> i.getBookId().equals(bookId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Item no encontrado: " + bookId));
    }

    // ── createFor() ───────────────────────────────────────────────────────

    @Test
    @DisplayName("createFor() produces deterministic CartId for the same userId")
    void createFor_deterministicId() {
        Cart c1 = Cart.createFor(USER_ID);
        Cart c2 = Cart.createFor(USER_ID);
        assertThat(c1.getId()).isEqualTo(c2.getId());
    }

    // ── addItem() ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("addItem() adds a new line and emits CartItemAddedEvent")
    void addItem_newLine() {
        Cart cart = newCart();
        add(cart, book("b1"), "ISBN-001", "Clean Code", price("29.99"), 2);

        assertThat(cart.getItems()).hasSize(1);
        CartItem it = item(cart, book("b1"));
        assertThat(it.getQuantity()).isEqualTo(2);
        assertThat(it.subtotal().amount()).isEqualByComparingTo("59.98");

        List<DomainEvent> events = cart.pullDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(CartItemAddedEvent.class);
    }

    @Test
    @DisplayName("addItem() merges quantity when same bookId already exists")
    void addItem_mergesQuantity() {
        Cart cart = newCart();
        add(cart, book("b1"), "ISBN-001", "Clean Code", price("29.99"), 2);
        cart.pullDomainEvents();

        add(cart, book("b1"), "ISBN-001", "Clean Code", price("29.99"), 3);

        assertThat(cart.getItems()).hasSize(1);
        assertThat(item(cart, book("b1")).getQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("addItem() throws DomainException when cart reaches MAX_ITEMS")
    void addItem_maxItems_shouldThrow() {
        Cart cart = newCart();
        for (int i = 0; i < 50; i++) {
            add(cart, book("b" + i), "ISBN-" + i, "Book " + i, price("10.00"), 1);
        }
        cart.pullDomainEvents();

        assertThatThrownBy(() ->
                add(cart, book("b999"), "ISBN-999", "Extra", price("10.00"), 1))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("50");
    }

    // ── removeItem() ──────────────────────────────────────────────────────

    @Test
    @DisplayName("removeItem() removes existing item and emits CartItemRemovedEvent")
    void removeItem_success() {
        Cart cart = newCart();
        add(cart, book("b1"), "ISBN-001", "Clean Code", price("29.99"), 1);
        cart.pullDomainEvents();

        cart.removeItem(book("b1"));

        assertThat(cart.getItems()).isEmpty();
        List<DomainEvent> events = cart.pullDomainEvents();
        assertThat(events).anyMatch(e -> e instanceof CartItemRemovedEvent);
    }

    @Test
    @DisplayName("removeItem() throws DomainException if bookId not in cart")
    void removeItem_notFound_shouldThrow() {
        Cart cart = newCart();
        assertThatThrownBy(() -> cart.removeItem(book("nonexistent")))
                .isInstanceOf(DomainException.class);
    }

    // ── updateItemQuantity() ──────────────────────────────────────────────

    @Test
    @DisplayName("updateItemQuantity(0) removes the item")
    void updateItemQuantity_zero_removes() {
        Cart cart = newCart();
        add(cart, book("b1"), "ISBN-001", "Book", price("10.00"), 3);
        cart.pullDomainEvents();

        cart.updateItemQuantity(book("b1"), 0);

        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    @DisplayName("updateItemQuantity() updates to new value")
    void updateItemQuantity_updatesCorrectly() {
        Cart cart = newCart();
        add(cart, book("b1"), "ISBN-001", "Book", price("10.00"), 3);
        cart.pullDomainEvents();

        cart.updateItemQuantity(book("b1"), 7);

        assertThat(item(cart, book("b1")).getQuantity()).isEqualTo(7);
    }

    // ── clear() ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("clear() empties the cart and emits CartClearedEvent")
    void clear_emptiesCart() {
        Cart cart = newCart();
        add(cart, book("b1"), "ISBN-001", "Book 1", price("10.00"), 1);
        add(cart, book("b2"), "ISBN-002", "Book 2", price("20.00"), 2);
        cart.pullDomainEvents();

        cart.clear();

        assertThat(cart.getItems()).isEmpty();
        List<DomainEvent> events = cart.pullDomainEvents();
        assertThat(events).anyMatch(e -> e instanceof CartClearedEvent);
    }

    // ── checkout() ────────────────────────────────────────────────────────

    @Test
    @DisplayName("checkout() emits CartCheckedOutEvent with correct total and clears items")
    void checkout_success() {
        Cart cart = newCart();
        add(cart, book("b1"), "ISBN-001", "Book 1", price("10.00"), 2);
        add(cart, book("b2"), "ISBN-002", "Book 2", price("5.00"), 1);
        cart.pullDomainEvents();

        cart.checkout("buyer@example.com", null, "CARD", "PHYSICAL", null);

        assertThat(cart.getItems()).isEmpty();

        List<DomainEvent> events = cart.pullDomainEvents();
        assertThat(events).hasSize(1);
        CartCheckedOutEvent event = (CartCheckedOutEvent) events.get(0);
        assertThat(event.total().amount()).isEqualByComparingTo("25.00");
        assertThat(event.items()).hasSize(2);
        assertThat(event.recipientEmail()).isEqualTo("buyer@example.com");
    }

    @Test
    @DisplayName("checkout() throws DomainException when cart is empty")
    void checkout_emptyCart_shouldThrow() {
        Cart cart = newCart();
        assertThatThrownBy(() -> cart.checkout("buyer@example.com", null, "CARD", "PHYSICAL", null))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("empty");
    }

    // ── total() ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("total() correctly sums all item subtotals")
    void total_calculatesCorrectly() {
        Cart cart = newCart();
        add(cart, book("b1"), "ISBN-001", "Book 1", price("15.00"), 3);
        add(cart, book("b2"), "ISBN-002", "Book 2", price("7.50"), 2);

        // 15 * 3 + 7.5 * 2 = 45 + 15 = 60
        assertThat(cart.total().amount()).isEqualByComparingTo("60.00");
    }

    @Test
    @DisplayName("total() returns zero for empty cart")
    void total_emptyCart_returnsZero() {
        Cart cart = newCart();
        assertThat(cart.total().amount()).isEqualByComparingTo("0.00");
    }
}
