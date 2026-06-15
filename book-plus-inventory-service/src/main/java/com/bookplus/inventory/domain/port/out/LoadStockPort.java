package com.bookplus.inventory.domain.port.out;

import com.bookplus.inventory.domain.model.BookId;
import com.bookplus.inventory.domain.model.Stock;

import java.util.Optional;

public interface LoadStockPort {
    Optional<Stock> findByBookId(BookId bookId);
    boolean existsByBookId(BookId bookId);
}
