package com.bookplus.order.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Nota con la que el admin resuelve un reclamo. */
public record ResolveClaimRequest(
        @NotBlank(message = "resolution is required") @Size(max = 500) String resolution
) {}
