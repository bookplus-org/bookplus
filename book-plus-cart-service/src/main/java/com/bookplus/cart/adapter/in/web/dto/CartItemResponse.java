package com.bookplus.cart.adapter.in.web.dto;

import com.bookplus.cart.domain.model.CartItem;

import java.math.BigDecimal;

public record CartItemResponse(
        String     bookId,
        String     isbn,
        String     title,
        String     imageUrl,
        BigDecimal unitPrice,
        String     currency,
        int        quantity,
        BigDecimal subtotal
) {
    public static CartItemResponse from(CartItem item) {
        return new CartItemResponse(
                item.getBookId().toString(),
                item.getIsbn(),
                item.getTitle(),
                item.getImageUrl(),
                item.getUnitPrice().amount(),
                item.getUnitPrice().currency(),
                item.getQuantity(),
                item.subtotal().amount()
        );
    }
}
