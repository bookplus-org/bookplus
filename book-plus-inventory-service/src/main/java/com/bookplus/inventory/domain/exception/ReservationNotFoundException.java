package com.bookplus.inventory.domain.exception;

public class ReservationNotFoundException extends DomainException {
    public ReservationNotFoundException(String identifier) {
        super("Reservation not found: " + identifier);
    }
}
