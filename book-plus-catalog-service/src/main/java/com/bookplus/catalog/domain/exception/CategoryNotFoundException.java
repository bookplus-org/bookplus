package com.bookplus.catalog.domain.exception;

public class CategoryNotFoundException extends DomainException {
    public CategoryNotFoundException(String identifier) {
        super("Category not found: " + identifier);
    }
}
