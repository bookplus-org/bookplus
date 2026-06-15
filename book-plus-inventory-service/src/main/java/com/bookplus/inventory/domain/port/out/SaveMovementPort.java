package com.bookplus.inventory.domain.port.out;

import com.bookplus.inventory.domain.model.StockMovement;

import java.util.List;

public interface SaveMovementPort {
    StockMovement save(StockMovement movement);
}
