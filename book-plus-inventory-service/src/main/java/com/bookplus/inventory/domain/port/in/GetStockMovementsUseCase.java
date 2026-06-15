package com.bookplus.inventory.domain.port.in;

import com.bookplus.inventory.domain.model.StockMovement;

import java.util.List;

public interface GetStockMovementsUseCase {
    List<StockMovement> getByBookId(String bookId, int page, int size);
}
