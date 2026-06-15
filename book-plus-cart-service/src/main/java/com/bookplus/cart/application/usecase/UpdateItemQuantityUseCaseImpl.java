package com.bookplus.cart.application.usecase;

import com.bookplus.cart.domain.exception.CartNotFoundException;
import com.bookplus.cart.domain.model.*;
import com.bookplus.cart.domain.port.in.UpdateItemQuantityUseCase;
import com.bookplus.cart.domain.port.out.*;
import com.bookplus.cart.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase @RequiredArgsConstructor @Slf4j
public class UpdateItemQuantityUseCaseImpl implements UpdateItemQuantityUseCase {

    private final LoadCartPort             loadCartPort;
    private final SaveCartPort             saveCartPort;
    private final DomainEventPublisherPort eventPublisher;

    @Override
    public Cart updateQuantity(String userId, String bookId, int quantity) {
        Cart cart = loadCartPort.findByUserId(userId)
                .orElseThrow(() -> new CartNotFoundException(userId));

        // quantity == 0 is interpreted as "remove item" by Cart aggregate
        cart.updateItemQuantity(BookId.of(bookId), quantity);
        Cart saved = saveCartPort.save(cart);

        try {
            eventPublisher.publishAll(cart.pullDomainEvents());
        } catch (Exception ex) {
            log.warn("Failed to publish cart quantity events: {}", ex.getMessage());
        }

        return saved;
    }
}
