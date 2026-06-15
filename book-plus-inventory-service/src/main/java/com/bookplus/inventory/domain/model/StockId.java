package com.bookplus.inventory.domain.model;

import java.util.Objects;
import java.util.UUID;

public record StockId(UUID value) {
    public StockId { Objects.requireNonNull(value, "StockId must not be null"); }
    public static StockId generate()       { return new StockId(UUID.randomUUID()); }
    public static StockId of(UUID value)   { return new StockId(value); }
    public static StockId of(String value) { return new StockId(UUID.fromString(value)); }
    @Override public String toString()     { return value.toString(); }
}
