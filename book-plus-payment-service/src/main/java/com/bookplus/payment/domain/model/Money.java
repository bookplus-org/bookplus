package com.bookplus.payment.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Money(BigDecimal amount, String currency) {
    public Money {
        if (amount == null) throw new IllegalArgumentException("amount must not be null");
        if (currency == null || currency.isBlank()) throw new IllegalArgumentException("currency must not be blank");
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }
    public static Money of(BigDecimal amount, String currency) { return new Money(amount, currency); }
    public static Money zero(String currency) { return new Money(BigDecimal.ZERO, currency); }
}
