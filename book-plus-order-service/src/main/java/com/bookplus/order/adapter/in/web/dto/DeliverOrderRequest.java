package com.bookplus.order.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Datos para confirmar la entrega: código que el cliente dio + quién recibió. */
public record DeliverOrderRequest(
        @NotBlank(message = "deliveryCode is required") String deliveryCode,
        String receivedBy
) {}
