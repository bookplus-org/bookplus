package com.bookplus.inventory.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Value Object — ID del libro (referencia desnormalizada al catalog-service). */
public record BookId(UUID value) {
    public BookId { Objects.requireNonNull(value, "BookId must not be null"); }
    public static BookId generate()       { return new BookId(UUID.randomUUID()); }
    public static BookId of(UUID value)   { return new BookId(value); }
    public static BookId of(String value) { return new BookId(UUID.fromString(value)); }
    @Override public String toString()    { return value.toString(); }
}
