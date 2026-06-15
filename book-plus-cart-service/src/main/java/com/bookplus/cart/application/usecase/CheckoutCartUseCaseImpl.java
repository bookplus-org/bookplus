package com.bookplus.cart.application.usecase;

import com.bookplus.cart.domain.event.CartCheckedOutEvent.ShippingAddressDto;
import com.bookplus.cart.domain.exception.CartNotFoundException;
import com.bookplus.cart.domain.model.Cart;
import com.bookplus.cart.domain.port.in.CheckoutCartUseCase;
import com.bookplus.cart.domain.port.out.*;
import com.bookplus.cart.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Checkout flow:
 *  1. Load cart (must exist and be non-empty — enforced by Cart.checkout())
 *  2. Call Cart.checkout() → emits CartCheckedOutEvent with full item snapshot + total
 *  3. Persist the now-empty cart (items cleared inside aggregate)
 *  4. Publish CartCheckedOutEvent → consumed by order-service to create the order
 *
 * Note: price synchronisation (syncItemPrice) is the responsibility of the caller
 * (CartController) before invoking checkout, so the event carries up-to-date prices.
 */
@UseCase @RequiredArgsConstructor @Slf4j
public class CheckoutCartUseCaseImpl implements CheckoutCartUseCase {

    private final LoadCartPort             loadCartPort;
    private final SaveCartPort             saveCartPort;
    private final DomainEventPublisherPort eventPublisher;

    @Override
    public void checkout(String userId, String recipientEmail, ShippingAddressDto shippingAddress,
                         String paymentMethod, String deliveryType, String couponCode) {
        Cart cart = loadCartPort.findByUserId(userId)
                .orElseThrow(() -> new CartNotFoundException(userId));

        cart.checkout(recipientEmail, shippingAddress, paymentMethod, deliveryType, couponCode); // emits event; clears items
        saveCartPort.save(cart);    // persist the cleared cart

        // Events MUST be published — order-service depends on CartCheckedOutEvent
        try {
            eventPublisher.publishAll(cart.pullDomainEvents());
        } catch (Exception ex) {
            // Log and rethrow: a failed publish means the order will never be created
            log.error("CRITICAL — failed to publish CartCheckedOutEvent for user {}: {}", userId, ex.getMessage());
            throw new RuntimeException("Checkout event publication failed", ex);
        }
    }
}
