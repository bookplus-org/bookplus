package com.bookplus.order.domain.model;

import lombok.Getter;

import java.util.UUID;

/**
 * Order line — immutable snapshot of a cart item at checkout time.
 * Price is frozen: subsequent catalog price changes do not affect existing orders.
 */
@Getter
public class OrderItem {

    private final UUID   id;
    private final String bookId;
    private final String isbn;
    private final String title;
    private final String imageUrl;
    private final Money  unitPrice;
    private final int    quantity;

    private OrderItem(UUID id, String bookId, String isbn, String title,
                      String imageUrl, Money unitPrice, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("OrderItem quantity must be positive");
        if (unitPrice.amount().signum() <= 0)
            throw new IllegalArgumentException("OrderItem unitPrice must be positive");
        this.id        = id;
        this.bookId    = bookId;
        this.isbn      = isbn;
        this.title     = title;
        this.imageUrl  = imageUrl;
        this.unitPrice = unitPrice;
        this.quantity  = quantity;
    }

    public static OrderItem create(String bookId, String isbn, String title,
                                   String imageUrl, Money unitPrice, int quantity) {
        return new OrderItem(UUID.randomUUID(), bookId, isbn, title, imageUrl, unitPrice, quantity);
    }

    public static OrderItem reconstitute(UUID id, String bookId, String isbn, String title,
                                         String imageUrl, Money unitPrice, int quantity) {
        return new OrderItem(id, bookId, isbn, title, imageUrl, unitPrice, quantity);
    }

    public Money subtotal() {
        return unitPrice.multiply(quantity);
    }
}
