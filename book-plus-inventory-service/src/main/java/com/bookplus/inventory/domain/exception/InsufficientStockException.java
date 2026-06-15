package com.bookplus.inventory.domain.exception;

import com.bookplus.inventory.domain.model.BookId;

public class InsufficientStockException extends DomainException {
    public InsufficientStockException(BookId bookId, int requested, int available) {
        super("Insufficient stock for book " + bookId.value()
              + ": requested=" + requested + " available=" + available);
    }
}
