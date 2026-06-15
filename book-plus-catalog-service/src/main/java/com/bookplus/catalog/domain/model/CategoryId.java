package com.bookplus.catalog.domain.model;

import java.util.Objects;
import java.util.UUID;

public record CategoryId(UUID value) {
    public CategoryId { Objects.requireNonNull(value, "CategoryId must not be null"); }
    public static CategoryId generate()       { return new CategoryId(UUID.randomUUID()); }
    public static CategoryId of(UUID value)   { return new CategoryId(value); }
    public static CategoryId of(String value) { return new CategoryId(UUID.fromString(value)); }
    @Override public String toString()        { return value.toString(); }
}
