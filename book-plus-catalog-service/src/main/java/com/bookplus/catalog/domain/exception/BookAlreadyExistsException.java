package com.bookplus.catalog.domain.exception;

public class BookAlreadyExistsException extends DomainException {
    public BookAlreadyExistsException(String isbn) {
        super("Book already exists with ISBN: " + isbn);
    }
}
