package com.bookplus.auth.domain.port.in;

import jakarta.validation.constraints.NotBlank;

/**
 * Puerto de entrada — Caso de uso: cerrar sesión.
 * Revoca el refresh token y agrega el access token a la blacklist (Redis).
 */
public interface LogoutUseCase {

    void logout(LogoutCommand command);

    record LogoutCommand(
            @NotBlank String refreshToken,
            @NotBlank String accessToken
    ) {}
}
