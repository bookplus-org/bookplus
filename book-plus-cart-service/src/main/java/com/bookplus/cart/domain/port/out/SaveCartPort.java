package com.bookplus.cart.domain.port.out;

import com.bookplus.cart.domain.model.Cart;

public interface SaveCartPort {
    Cart save(Cart cart);
    void delete(String userId);
}
