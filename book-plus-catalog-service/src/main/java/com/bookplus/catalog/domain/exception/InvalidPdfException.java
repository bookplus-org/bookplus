package com.bookplus.catalog.domain.exception;

/** El PDF subido es inválido: vacío, sin páginas o dañado. Se mapea a 400 Bad Request. */
public class InvalidPdfException extends DomainException {
    public InvalidPdfException(String message) {
        super(message);
    }
}
