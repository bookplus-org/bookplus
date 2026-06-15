package com.bookplus.payment.domain.model;

import java.util.UUID;

public record PaymentId(UUID value) {
    public static PaymentId generate()    { return new PaymentId(UUID.randomUUID()); }
    public static PaymentId of(String id) { return new PaymentId(UUID.fromString(id)); }
    public static PaymentId of(UUID id)   { return new PaymentId(id); }
    @Override public String toString()    { return value.toString(); }
}
