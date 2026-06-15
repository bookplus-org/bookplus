package com.bookplus.catalog.domain.exception;

public class BookNotFoundException extends DomainException {
    public BookNotFoundException(String identifier) {
        super("Book not found: " + identifier);
    }
}
