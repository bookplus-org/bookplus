package com.bookplus.auth.domain.exception;

public class UserNotFoundException extends DomainException {
    public UserNotFoundException(String identifier) {
        super("User not found: " + identifier);
    }
}
