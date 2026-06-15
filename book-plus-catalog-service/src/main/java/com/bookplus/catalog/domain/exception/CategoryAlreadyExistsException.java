package com.bookplus.catalog.domain.exception;

public class CategoryAlreadyExistsException extends DomainException {
    public CategoryAlreadyExistsException(String name) {
        super("Category already exists with name: " + name);
    }
}
