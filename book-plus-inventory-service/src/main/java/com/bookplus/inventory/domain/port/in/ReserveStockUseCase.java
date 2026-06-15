package com.bookplus.inventory.domain.port.in;

import com.bookplus.inventory.domain.model.StockReservation;

public interface ReserveStockUseCase {
    StockReservation reserve(ReserveStockCommand command);

    record ReserveStockCommand(String bookId, String orderId, String userId, int quantity) {}
}
