package com.bookplus.auth.domain.port.in;

import jakarta.validation.constraints.NotBlank;

/**
 * Puerto de entrada — Caso de uso: renovar access token con refresh token.
 * Implementa rotación: el refresh token viejo se revoca y se emite uno nuevo.
 */
public interface RefreshTokenUseCase {

    AuthResult refresh(RefreshCommand command);

    record RefreshCommand(
            @NotBlank String refreshToken,
            String ipAddress,
            String userAgent
    ) {}
}
