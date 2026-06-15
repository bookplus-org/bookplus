package com.bookplus.catalog.domain.model;

import com.bookplus.catalog.domain.exception.DomainException;

/**
 * Value Object — Calificación de reseña (1 a 5 estrellas).
 */
public record Rating(int value) {

    public Rating {
        if (value < 1 || value > 5) {
            throw new DomainException("Rating must be between 1 and 5, got: " + value);
        }
    }

    public static Rating of(int value) { return new Rating(value); }
    public boolean isPositive()        { return value >= 4; }
    @Override public String toString() { return String.valueOf(value); }
}
