package com.bookplus.cart.domain.port.in;

import com.bookplus.cart.domain.model.Cart;

public interface UpdateItemQuantityUseCase {
    Cart updateQuantity(String userId, String bookId, int quantity);
}
