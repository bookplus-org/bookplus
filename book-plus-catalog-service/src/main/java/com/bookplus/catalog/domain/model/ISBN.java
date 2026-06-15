package com.bookplus.catalog.domain.model;

import com.bookplus.catalog.domain.exception.DomainException;

import java.util.Objects;

/**
 * Value Object — ISBN-13.
 * Valida formato y dígito de control según el estándar ISO 2108.
 */
public record ISBN(String value) {

    public ISBN {
        Objects.requireNonNull(value, "ISBN must not be null");
        String digits = value.replaceAll("[\\s-]", "");
        if (!isValidIsbn13(digits)) {
            throw new DomainException("Invalid ISBN-13: " + value);
        }
        value = digits;
    }

    public static ISBN of(String value) {
        return new ISBN(value);
    }

    private static boolean isValidIsbn13(String digits) {
        if (digits.length() != 13 || !digits.matches("\\d+")) return false;
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int d = digits.charAt(i) - '0';
            sum += (i % 2 == 0) ? d : d * 3;
        }
        int checkDigit = (10 - (sum % 10)) % 10;
        return checkDigit == (digits.charAt(12) - '0');
    }

    @Override public String toString() { return value; }
}
