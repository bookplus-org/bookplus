package com.bookplus.cart.application.usecase;

import com.bookplus.cart.domain.model.*;
import com.bookplus.cart.domain.port.in.AddItemToCartUseCase;
import com.bookplus.cart.domain.port.out.*;
import com.bookplus.cart.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase @RequiredArgsConstructor @Slf4j
public class AddItemToCartUseCaseImpl implements AddItemToCartUseCase {

    private final LoadCartPort             loadCartPort;
    private final SaveCartPort             saveCartPort;
    private final DomainEventPublisherPort eventPublisher;

    @Override
    public Cart addItem(AddItemCommand command) {
        log.debug("Adding item to cart: userId={} bookId={} qty={}",
                command.userId(), command.bookId(), command.quantity());

        // Cargar o crear carrito
        Cart cart = loadCartPort.findByUserId(command.userId())
                .orElseGet(() -> Cart.createFor(command.userId()));

        Money unitPrice = command.unitPrice();

        cart.addItem(
                BookId.of(command.bookId()),
                command.title(),
                command.imageUrl(),
                command.isbn(),
                command.quantity(),
                unitPrice
        );

        Cart saved = saveCartPort.save(cart);

        try {
            eventPublisher.publishAll(cart.pullDomainEvents());
        } catch (Exception ex) {
            log.warn("Failed to publish CartItemAddedEvent: {}", ex.getMessage());
        }

        return saved;
    }
}
