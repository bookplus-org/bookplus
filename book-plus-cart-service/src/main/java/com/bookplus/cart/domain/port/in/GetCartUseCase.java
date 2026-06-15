package com.bookplus.cart.domain.port.in;

import com.bookplus.cart.domain.model.Cart;

public interface GetCartUseCase {
    /** Devuelve el carrito del usuario, creándolo vacío si no existe. */
    Cart getOrCreate(String userId);
}
