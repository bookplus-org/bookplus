package com.bookplus.cart.domain.model;

import com.bookplus.cart.domain.exception.DomainException;

import java.util.Objects;

/**
 * Entity — ítem dentro del carrito.
 * Contiene una snapshot del precio del libro en el momento de añadir.
 * El precio se puede actualizar con syncPrice() si el catálogo cambia.
 */
public class CartItem {

    private final CartItemId id;
    private final BookId     bookId;
    private final String     title;        // desnormalizado de catalog-service
    private final String     imageUrl;     // desnormalizado
    private final String     isbn;         // desnormalizado
    private       int        quantity;
    private       Money      unitPrice;    // precio snapshot al momento de añadir

    private CartItem(CartItemId id, BookId bookId, String title,
                     String imageUrl, String isbn, int quantity, Money unitPrice) {
        this.id        = Objects.requireNonNull(id);
        this.bookId    = Objects.requireNonNull(bookId);
        this.title     = Objects.requireNonNull(title);
        this.imageUrl  = imageUrl;
        this.isbn      = isbn;
        this.quantity  = validateQuantity(quantity);
        this.unitPrice = Objects.requireNonNull(unitPrice);
    }

    public static CartItem create(BookId bookId, String title, String imageUrl,
                                  String isbn, int quantity, Money unitPrice) {
        return new CartItem(CartItemId.generate(), bookId, title, imageUrl, isbn, quantity, unitPrice);
    }

    public static CartItem reconstitute(CartItemId id, BookId bookId, String title,
                                        String imageUrl, String isbn,
                                        int quantity, Money unitPrice) {
        return new CartItem(id, bookId, title, imageUrl, isbn, quantity, unitPrice);
    }

    // ── Comportamientos ───────────────────────────────────────────────────

    public void increaseQuantity(int delta) {
        this.quantity = validateQuantity(this.quantity + delta);
    }

    public void setQuantity(int quantity) {
        this.quantity = validateQuantity(quantity);
    }

    /** Actualiza el precio unitario si el catálogo lo ha cambiado. */
    public void syncPrice(Money newUnitPrice) {
        this.unitPrice = Objects.requireNonNull(newUnitPrice);
    }

    public Money subtotal() { return unitPrice.multiply(quantity); }

    private static int validateQuantity(int qty) {
        if (qty <= 0) throw new DomainException("Cart item quantity must be positive, got: " + qty);
        if (qty > 99) throw new DomainException("Cart item quantity cannot exceed 99 per item");
        return qty;
    }

    public CartItemId getId()       { return id; }
    public BookId     getBookId()   { return bookId; }
    public String     getTitle()    { return title; }
    public String     getImageUrl() { return imageUrl; }
    public String     getIsbn()     { return isbn; }
    public int        getQuantity() { return quantity; }
    public Money      getUnitPrice(){ return unitPrice; }
}
