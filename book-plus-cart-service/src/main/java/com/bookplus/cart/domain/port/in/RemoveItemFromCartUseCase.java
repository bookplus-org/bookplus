package com.bookplus.cart.domain.port.in;

import com.bookplus.cart.domain.model.Cart;

public interface RemoveItemFromCartUseCase {
    Cart removeItem(String userId, String bookId);
}
