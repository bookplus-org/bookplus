package com.bookplus.inventory.domain.port.out;

import com.bookplus.inventory.domain.model.Stock;

public interface SaveStockPort {
    Stock save(Stock stock);
}
