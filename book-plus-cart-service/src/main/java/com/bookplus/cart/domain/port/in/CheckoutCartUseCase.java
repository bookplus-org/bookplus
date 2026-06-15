package com.bookplus.cart.domain.port.in;

import com.bookplus.cart.domain.event.CartCheckedOutEvent.ShippingAddressDto;

/** Inicia el proceso de checkout — publica CartCheckedOutEvent para el order-service. */
public interface CheckoutCartUseCase {
    void checkout(String userId, String recipientEmail, ShippingAddressDto shippingAddress,
                  String paymentMethod, String deliveryType, String couponCode);
}
