package com.bookplus.cart.application.usecase;

import com.bookplus.cart.domain.exception.CartNotFoundException;
import com.bookplus.cart.domain.model.Cart;
import com.bookplus.cart.domain.port.in.ClearCartUseCase;
import com.bookplus.cart.domain.port.out.*;
import com.bookplus.cart.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase @RequiredArgsConstructor @Slf4j
public class ClearCartUseCaseImpl implements ClearCartUseCase {

    private final LoadCartPort             loadCartPort;
    private final SaveCartPort             saveCartPort;
    private final DomainEventPublisherPort eventPublisher;

    @Override
    public void clear(String userId) {
        Cart cart = loadCartPort.findByUserId(userId)
                .orElseThrow(() -> new CartNotFoundException(userId));

        cart.clear();
        saveCartPort.save(cart);

        try {
            eventPublisher.publishAll(cart.pullDomainEvents());
        } catch (Exception ex) {
            log.warn("Failed to publish CartClearedEvent: {}", ex.getMessage());
        }
    }
}
