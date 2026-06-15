package com.bookplus.order.domain.model;

/**
 * Value Object — shipping address snapshot embedded in the Order aggregate.
 * Immutable: once the order is placed the address is frozen.
 */
public record ShippingAddress(
        String recipientName,
        String street,
        String city,
        String state,
        String postalCode,
        String country
) {
    public ShippingAddress {
        if (recipientName == null || recipientName.isBlank())
            throw new IllegalArgumentException("recipientName is required");
        if (street == null || street.isBlank())
            throw new IllegalArgumentException("street is required");
        if (city == null || city.isBlank())
            throw new IllegalArgumentException("city is required");
        if (postalCode == null || postalCode.isBlank())
            throw new IllegalArgumentException("postalCode is required");
        if (country == null || country.isBlank())
            throw new IllegalArgumentException("country is required");
    }
}
