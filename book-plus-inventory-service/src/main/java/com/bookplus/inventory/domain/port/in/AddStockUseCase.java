package com.bookplus.inventory.domain.port.in;

import com.bookplus.inventory.domain.model.Stock;

public interface AddStockUseCase {
    Stock addStock(AddStockCommand command);

    record AddStockCommand(String bookId, int quantity, String referenceId, String notes) {}
}
