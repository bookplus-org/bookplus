package com.bookplus.inventory.domain.port.in;

import com.bookplus.inventory.domain.model.Stock;

public interface GetStockUseCase {
    Stock getByBookId(String bookId);
}
