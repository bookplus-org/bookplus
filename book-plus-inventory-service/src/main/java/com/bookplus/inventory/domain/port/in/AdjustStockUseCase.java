package com.bookplus.inventory.domain.port.in;

import com.bookplus.inventory.domain.model.Stock;

public interface AdjustStockUseCase {
    Stock adjust(AdjustStockCommand command);

    record AdjustStockCommand(String bookId, int newTotalQuantity,
                               int lowStockThreshold, String notes) {}
}
