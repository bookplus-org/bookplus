package com.bookplus.cart.application.usecase;

import com.bookplus.cart.domain.model.Cart;
import com.bookplus.cart.domain.port.in.GetCartUseCase;
import com.bookplus.cart.domain.port.out.LoadCartPort;
import com.bookplus.cart.domain.port.out.SaveCartPort;
import com.bookplus.cart.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase @RequiredArgsConstructor @Slf4j
public class GetCartUseCaseImpl implements GetCartUseCase {

    private final LoadCartPort loadCartPort;
    private final SaveCartPort saveCartPort;

    @Override
    public Cart getOrCreate(String userId) {
        return loadCartPort.findByUserId(userId).orElseGet(() -> {
            log.debug("No cart found for user={}, creating empty cart", userId);
            Cart newCart = Cart.createFor(userId);
            return saveCartPort.save(newCart);
        });
    }
}
