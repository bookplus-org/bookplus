package com.bookplus.order.adapter.in.web.dto;

import com.bookplus.order.domain.model.OrderItem;

import java.math.BigDecimal;

public record OrderItemResponse(
        String     bookId,
        String     isbn,
        String     title,
        String     imageUrl,
        BigDecimal unitPrice,
        String     currency,
        int        quantity,
        BigDecimal subtotal
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getBookId(),
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
