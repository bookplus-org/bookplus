package com.bookplus.order.adapter.in.web.dto;

/**
 * Datos del envío. Para reparto propio (personal de la tienda) la paquetería y
 * el número de seguimiento pueden ir vacíos.
 */
public record ShipOrderRequest(
        String carrier,
        String trackingNumber
) {}
