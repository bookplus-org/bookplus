package com.bookplus.catalog.domain.model;

import java.util.Objects;
import java.util.UUID;

public record ReviewId(UUID value) {
    public ReviewId { Objects.requireNonNull(value, "ReviewId must not be null"); }
    public static ReviewId generate()       { return new ReviewId(UUID.randomUUID()); }
    public static ReviewId of(UUID value)   { return new ReviewId(value); }
    public static ReviewId of(String value) { return new ReviewId(UUID.fromString(value)); }
    @Override public String toString()      { return value.toString(); }
}
