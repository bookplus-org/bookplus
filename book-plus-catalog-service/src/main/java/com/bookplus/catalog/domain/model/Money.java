package com.bookplus.catalog.domain.model;

import com.bookplus.catalog.domain.exception.DomainException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value Object — Precio monetario.
 * Inmutable, escala fija de 2 decimales, siempre no-negativo.
 */
public record Money(BigDecimal amount, String currency) {

    public Money {
        Objects.requireNonNull(amount,   "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        amount = amount.setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new DomainException("Money amount cannot be negative: " + amount);
        }
    }

    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }

    public static Money ofUSD(BigDecimal amount) { return new Money(amount, "USD"); }
    public static Money ofPEN(BigDecimal amount) { return new Money(amount, "PEN"); }
    public static Money zero(String currency)    { return new Money(BigDecimal.ZERO, currency); }

    public boolean isGreaterThan(Money other) {
        assertSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    public Money subtract(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    private void assertSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new DomainException("Currency mismatch: %s vs %s".formatted(this.currency, other.currency));
        }
    }

    @Override public String toString() { return "%s %s".formatted(amount.toPlainString(), currency); }
}
