package com.bookplus.inventory.domain.port.out;

import com.bookplus.inventory.domain.model.BookId;
import com.bookplus.inventory.domain.model.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LoadMovementPort {
    Page<StockMovement> findByBookId(BookId bookId, Pageable pageable);
}
