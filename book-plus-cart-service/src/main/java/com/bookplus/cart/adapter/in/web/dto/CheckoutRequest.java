package com.bookplus.cart.adapter.in.web.dto;

import com.bookplus.cart.domain.event.CartCheckedOutEvent.ShippingAddressDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Cuerpo del checkout: dirección de envío + método de pago elegido en el frontend.
 * paymentReference es opcional (últimos 4 de tarjeta, n° de operación Yape/Plin, etc.).
 */
public record CheckoutRequest(
        // Solo obligatoria si la entrega es física (validado en el controlador).
        @Valid ShippingAddress shippingAddress,
        @NotBlank @Pattern(regexp = "YAPE|PLIN|CARD|CASH|PAYPAL",
                message = "paymentMethod debe ser YAPE, PLIN, CARD, CASH o PAYPAL") String paymentMethod,
        String paymentReference,
        @NotBlank @Pattern(regexp = "DIGITAL|PHYSICAL",
                message = "deliveryType debe ser DIGITAL o PHYSICAL") String deliveryType,
        String couponCode
) {

    public record ShippingAddress(
            @NotBlank String recipientName,
            @NotBlank String street,
            @NotBlank String city,
            String state,
            @NotBlank String postalCode,
            @NotBlank String country
    ) {}

    public boolean isPhysical() {
        return "PHYSICAL".equals(deliveryType);
    }

    /** Convierte el DTO web al DTO de dominio. Para entrega digital usa un marcador. */
    public ShippingAddressDto toShippingAddressDto() {
        if (shippingAddress == null) {
            return new ShippingAddressDto("Entrega digital", "—", "—", "", "00000", "—");
        }
        return new ShippingAddressDto(
                shippingAddress.recipientName(),
                shippingAddress.street(),
                shippingAddress.city(),
                shippingAddress.state(),
                shippingAddress.postalCode(),
                shippingAddress.country()
        );
    }
}
