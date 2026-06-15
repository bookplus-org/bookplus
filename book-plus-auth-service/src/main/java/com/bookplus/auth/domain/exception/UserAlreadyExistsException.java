package com.bookplus.auth.domain.exception;

public class UserAlreadyExistsException extends DomainException {
    public UserAlreadyExistsException(String field, String value) {
        super("User already exists with %s: %s".formatted(field, value));
    }
}
