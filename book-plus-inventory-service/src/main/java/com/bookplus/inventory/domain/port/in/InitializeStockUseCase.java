package com.bookplus.inventory.domain.port.in;

import com.bookplus.inventory.domain.model.Stock;

public interface InitializeStockUseCase {
    Stock initialize(InitializeStockCommand command);

    record InitializeStockCommand(String bookId, int initialQuantity, int lowStockThreshold) {}
}
