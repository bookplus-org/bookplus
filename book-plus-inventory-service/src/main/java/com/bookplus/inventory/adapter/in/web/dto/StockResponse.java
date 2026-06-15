package com.bookplus.inventory.adapter.in.web.dto;

import com.bookplus.inventory.domain.model.Stock;

import java.time.Instant;

public record StockResponse(
        String  id,
        String  bookId,
        int     quantityTotal,
        int     quantityAvailable,
        int     quantityReserved,
        int     lowStockThreshold,
        boolean inStock,
        boolean lowStock,
        Instant createdAt,
        Instant updatedAt
) {
    public static StockResponse from(Stock s) {
        return new StockResponse(
                s.getId().value().toString(),
                s.getBookId().value().toString(),
                s.getQuantityTotal(),
                s.getQuantityAvailable(),
                s.getQuantityReserved(),
                s.getLowStockThreshold(),
                !s.isOutOfStock(),
                s.isLowStock(),
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }
}
