package com.bookplus.order.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Motivo del reclamo abierto por el cliente. */
public record ClaimRequest(
        @NotBlank(message = "reason is required") @Size(max = 500) String reason
) {}
