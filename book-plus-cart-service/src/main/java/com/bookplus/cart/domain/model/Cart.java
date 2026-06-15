package com.bookplus.cart.domain.model;

import com.bookplus.cart.domain.event.*;
import com.bookplus.cart.domain.exception.DomainException;

import java.time.Instant;
import java.util.*;

/**
 * Aggregate Root — Carrito de compras.
 *
 * Invariantes:
 *   - Un usuario tiene exactamente un carrito activo.
 *   - Máximo MAX_ITEMS líneas distintas.
 *   - Cada línea tiene entre 1 y 99 unidades.
 *   - El carrito expirado no puede ser modificado.
 *
 * Persistencia: Redis con TTL (carrito como sesión temporal).
 * No hay base de datos relacional para el carrito.
 */
public class Cart {

    private static final int MAX_ITEMS = 50;

    private final CartId          id;
    private final String          userId;
    private final Map<BookId, CartItem> items;    // bookId → item (1 línea por libro)
    private       Instant         createdAt;
    private       Instant         updatedAt;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    // ── Constructor ───────────────────────────────────────────────────────

    private Cart(CartId id, String userId, Map<BookId, CartItem> items,
                 Instant createdAt, Instant updatedAt) {
        this.id        = Objects.requireNonNull(id);
        this.userId    = Objects.requireNonNull(userId);
        this.items     = new LinkedHashMap<>(items);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    // ── Factory Methods ───────────────────────────────────────────────────

    /** Crea un carrito vacío para el usuario. */
    public static Cart createFor(String userId) {
        Instant now = Instant.now();
        return new Cart(CartId.forUser(userId), userId, new LinkedHashMap<>(), now, now);
    }

    public static Cart reconstitute(CartId id, String userId, Map<BookId, CartItem> items,
                                    Instant createdAt, Instant updatedAt) {
        return new Cart(id, userId, items, createdAt, updatedAt);
    }

    // ── Comportamientos de Dominio ────────────────────────────────────────

    /**
     * Añade un libro al carrito.
     * Si ya existe, incrementa la cantidad.
     */
    public void addItem(BookId bookId, String title, String imageUrl, String isbn,
                        int quantity, Money unitPrice) {
        if (items.containsKey(bookId)) {
            CartItem existing = items.get(bookId);
            existing.increaseQuantity(quantity);
        } else {
            if (items.size() >= MAX_ITEMS) {
                throw new DomainException(
                        "Cart cannot have more than " + MAX_ITEMS + " different items");
            }
            items.put(bookId, CartItem.create(bookId, title, imageUrl, isbn, quantity, unitPrice));
        }
        this.updatedAt = Instant.now();
        registerEvent(new CartItemAddedEvent(id, userId, bookId, quantity, unitPrice));
    }

    /**
     * Elimina un libro del carrito por completo.
     */
    public void removeItem(BookId bookId) {
        CartItem removed = items.remove(bookId);
        if (removed == null) {
            throw new DomainException("Item not found in cart: bookId=" + bookId);
        }
        this.updatedAt = Instant.now();
        registerEvent(new CartItemRemovedEvent(id, userId, bookId));
    }

    /**
     * Actualiza la cantidad de un ítem existente.
     * Si quantity == 0, elimina el ítem.
     */
    public void updateItemQuantity(BookId bookId, int quantity) {
        if (quantity == 0) {
            removeItem(bookId);
            return;
        }
        CartItem item = items.get(bookId);
        if (item == null) {
            throw new DomainException("Item not found in cart: bookId=" + bookId);
        }
        item.setQuantity(quantity);
        this.updatedAt = Instant.now();
    }

    /**
     * Sincroniza el precio de un ítem con el precio actual del catálogo.
     * Llamado cuando se detecta un cambio de precio antes del checkout.
     */
    public void syncItemPrice(BookId bookId, Money newUnitPrice) {
        CartItem item = items.get(bookId);
        if (item != null) {
            item.syncPrice(newUnitPrice);
            this.updatedAt = Instant.now();
        }
    }

    /**
     * Vacía el carrito completamente.
     */
    public void clear() {
        items.clear();
        this.updatedAt = Instant.now();
        registerEvent(new CartClearedEvent(id, userId));
    }

    /**
     * Marca el carrito como procesado al iniciar el checkout.
     * Publica el evento que el order-service consumirá.
     */
    public void checkout(String recipientEmail, CartCheckedOutEvent.ShippingAddressDto shippingAddress,
                         String paymentMethod, String deliveryType, String couponCode) {
        if (items.isEmpty()) {
            throw new DomainException("Cannot checkout an empty cart");
        }
        registerEvent(new CartCheckedOutEvent(id, userId, recipientEmail, new ArrayList<>(items.values()), total(),
                shippingAddress, paymentMethod, deliveryType, couponCode));
        // Tras checkout, el carrito se limpia (order-service crea la orden)
        items.clear();
        this.updatedAt = Instant.now();
    }

    // ── Queries ────────────────────────────────────────────────────────────

    public Money total() {
        if (items.isEmpty()) return Money.zero("USD");
        return items.values().stream()
                .map(CartItem::subtotal)
                .reduce(Money.zero(items.values().iterator().next().getUnitPrice().currency()),
                        Money::add);
    }

    public boolean isEmpty()           { return items.isEmpty(); }
    public int     itemCount()         { return items.size(); }
    public int     totalUnits()        { return items.values().stream().mapToInt(CartItem::getQuantity).sum(); }
    public boolean containsBook(BookId bookId) { return items.containsKey(bookId); }

    // ── Domain Events ─────────────────────────────────────────────────────

    private void registerEvent(DomainEvent event) { domainEvents.add(event); }

    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return Collections.unmodifiableList(events);
    }

    // ── Getters ───────────────────────────────────────────────────────────

    public CartId            getId()        { return id; }
    public String            getUserId()    { return userId; }
    public Collection<CartItem> getItems()  { return Collections.unmodifiableCollection(items.values()); }
    public Instant           getCreatedAt() { return createdAt; }
    public Instant           getUpdatedAt() { return updatedAt; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cart c)) return false;
        return id.equals(c.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
    @Override public String toString() {
        return "Cart{userId='%s', items=%d, total=%s}".formatted(userId, items.size(), total());
    }
}
