package com.bookplus.inventory.adapter.in.web.dto;

import com.bookplus.inventory.domain.model.MovementType;
import com.bookplus.inventory.domain.model.StockMovement;

import java.time.Instant;

public record MovementResponse(
        String       id,
        String       bookId,
        MovementType type,
        int          quantity,
        int          stockBefore,
        int          stockAfter,
        String       referenceId,
        String       notes,
        Instant      occurredAt
) {
    public static MovementResponse from(StockMovement m) {
        return new MovementResponse(
                m.getId().value().toString(),
                m.getBookId().value().toString(),
                m.getType(),
                m.getQuantity(),
                m.getStockBefore(),
                m.getStockAfter(),
                m.getReferenceId(),
                m.getNotes(),
                m.getOccurredAt()
        );
    }
}
