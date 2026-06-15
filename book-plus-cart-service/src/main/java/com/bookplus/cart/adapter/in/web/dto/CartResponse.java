package com.bookplus.cart.adapter.in.web.dto;

import com.bookplus.cart.domain.model.Cart;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CartResponse(
        String              cartId,
        String              userId,
        List<CartItemResponse> items,
        int                 itemCount,
        BigDecimal          total,
        String              currency,
        Instant             updatedAt
) {
    public static CartResponse from(Cart cart) {
        List<CartItemResponse> items = cart.getItems()
                .stream()
                .map(CartItemResponse::from)
                .toList();

        return new CartResponse(
                cart.getId().toString(),
                cart.getUserId(),
                items,
                cart.getItems().size(),
                cart.total().amount(),
                cart.total().currency(),
                cart.getUpdatedAt()
        );
    }
}
