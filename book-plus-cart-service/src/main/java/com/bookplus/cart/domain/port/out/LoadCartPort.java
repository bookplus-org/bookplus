package com.bookplus.cart.domain.port.out;

import com.bookplus.cart.domain.model.Cart;
import com.bookplus.cart.domain.model.CartId;

import java.util.Optional;

public interface LoadCartPort {
    Optional<Cart> findByUserId(String userId);
    Optional<Cart> findById(CartId cartId);
}
