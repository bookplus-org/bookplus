package com.bookplus.order.domain.model;

import java.util.UUID;

public record OrderId(UUID value) {
    public static OrderId generate()          { return new OrderId(UUID.randomUUID()); }
    public static OrderId of(String id)       { return new OrderId(UUID.fromString(id)); }
    public static OrderId of(UUID id)         { return new OrderId(id); }
    @Override public String toString()        { return value.toString(); }
}
