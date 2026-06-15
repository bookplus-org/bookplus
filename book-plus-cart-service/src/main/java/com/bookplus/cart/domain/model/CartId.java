package com.bookplus.cart.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Value Object — ID del carrito. El carrito se identifica por userId (1 carrito por usuario). */
public record CartId(UUID value) {
    public CartId { Objects.requireNonNull(value, "CartId must not be null"); }
    public static CartId generate()       { return new CartId(UUID.randomUUID()); }
    public static CartId of(UUID value)   { return new CartId(value); }
    public static CartId of(String value) { return new CartId(UUID.fromString(value)); }
    /** Crea un CartId determinístico a partir del userId para garantizar 1 carrito por usuario. */
    public static CartId forUser(String userId) {
        return new CartId(UUID.nameUUIDFromBytes(("cart:" + userId).getBytes()));
    }
    @Override public String toString() { return value.toString(); }
}
