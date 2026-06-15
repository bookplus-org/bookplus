package com.bookplus.inventory.domain.model;

import java.util.Objects;
import java.util.UUID;

public record MovementId(UUID value) {
    public MovementId { Objects.requireNonNull(value, "MovementId must not be null"); }
    public static MovementId generate()       { return new MovementId(UUID.randomUUID()); }
    public static MovementId of(UUID value)   { return new MovementId(value); }
    public static MovementId of(String value) { return new MovementId(UUID.fromString(value)); }
    @Override public String toString()        { return value.toString(); }
}
