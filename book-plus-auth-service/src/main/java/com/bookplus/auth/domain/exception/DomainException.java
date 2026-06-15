package com.bookplus.auth.domain.exception;

/** Base de todas las excepciones de dominio. */
public class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }
    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
