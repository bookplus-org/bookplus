package com.bookplus.cart.domain.model;

import com.bookplus.cart.domain.exception.DomainException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Money(BigDecimal amount, String currency) {

    public Money {
        Objects.requireNonNull(amount, "Amount must not be null");
        Objects.requireNonNull(currency, "Currency must not be null");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new DomainException("Money amount cannot be negative: " + amount);
        }
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money of(BigDecimal amount, String currency) { return new Money(amount, currency); }
    public static Money zero(String currency)                  { return new Money(BigDecimal.ZERO, currency); }

    public Money multiply(int factor) { return new Money(amount.multiply(BigDecimal.valueOf(factor)), currency); }
    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new DomainException("Cannot add different currencies: " + currency + " vs " + other.currency);
        }
        return new Money(this.amount.add(other.amount), currency);
    }

    @Override public String toString() { return amount.toPlainString() + " " + currency; }
}
