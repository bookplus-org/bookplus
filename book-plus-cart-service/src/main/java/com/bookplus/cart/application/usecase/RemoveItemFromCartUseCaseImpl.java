package com.bookplus.cart.application.usecase;

import com.bookplus.cart.domain.exception.CartNotFoundException;
import com.bookplus.cart.domain.model.*;
import com.bookplus.cart.domain.port.in.RemoveItemFromCartUseCase;
import com.bookplus.cart.domain.port.out.*;
import com.bookplus.cart.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase @RequiredArgsConstructor @Slf4j
public class RemoveItemFromCartUseCaseImpl implements RemoveItemFromCartUseCase {

    private final LoadCartPort             loadCartPort;
    private final SaveCartPort             saveCartPort;
    private final DomainEventPublisherPort eventPublisher;

    @Override
    public Cart removeItem(String userId, String bookId) {
        Cart cart = loadCartPort.findByUserId(userId)
                .orElseThrow(() -> new CartNotFoundException(userId));

        cart.removeItem(BookId.of(bookId));
        Cart saved = saveCartPort.save(cart);

        try {
            eventPublisher.publishAll(cart.pullDomainEvents());
        } catch (Exception ex) {
            log.warn("Failed to publish CartItemRemovedEvent: {}", ex.getMessage());
        }

        return saved;
    }
}
