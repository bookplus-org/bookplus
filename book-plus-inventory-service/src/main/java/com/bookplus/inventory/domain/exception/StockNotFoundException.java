package com.bookplus.inventory.domain.exception;

public class StockNotFoundException extends DomainException {
    public StockNotFoundException(String identifier) {
        super("Stock not found for: " + identifier);
    }
}
