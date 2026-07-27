package com.bookplus.catalog.domain.exception;

/** El usuario no tiene acceso vigente al libro (no comprado o revocado). Se mapea a 403. */
public class PurchaseAccessDeniedException extends DomainException {
    public PurchaseAccessDeniedException(String message) {
        super(message);
    }
}
