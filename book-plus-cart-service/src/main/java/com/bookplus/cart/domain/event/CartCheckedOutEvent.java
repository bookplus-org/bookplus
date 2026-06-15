package com.bookplus.cart.domain.event;

import com.bookplus.cart.domain.model.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Kafka topic: cart.checked-out
 *
 * Consumido por order-service para crear la orden.
 * Incluye el snapshot completo de los ítems, la dirección de envío y el método de pago.
 */
public record CartCheckedOutEvent(
        UUID            eventId,
        String          eventType,
        Instant         occurredOn,
        CartId          cartId,
        String          userId,
        String          recipientEmail,  // email del comprador (para notificaciones)
        List<CartItem>  items,
        Money           total,
        ShippingAddressDto shippingAddress,
        String          paymentMethod,
        String          deliveryType,  // DIGITAL | PHYSICAL
        String          couponCode
) implements DomainEvent {

    /** Snapshot de la dirección de envío elegida en el checkout. */
    public record ShippingAddressDto(
            String recipientName,
            String street,
            String city,
            String state,
            String postalCode,
            String country
    ) {}

    public CartCheckedOutEvent(CartId cartId, String userId, String recipientEmail,
                                List<CartItem> items, Money total,
                                ShippingAddressDto shippingAddress, String paymentMethod,
                                String deliveryType, String couponCode) {
        this(UUID.randomUUID(), "CART_CHECKED_OUT", Instant.now(),
             cartId, userId, recipientEmail, List.copyOf(items), total, shippingAddress,
             paymentMethod, deliveryType, couponCode);
    }
}
