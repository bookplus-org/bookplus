package com.bookplus.auth.domain.port.in;

import jakarta.validation.constraints.NotBlank;

/**
 * Puerto de entrada — Caso de uso: autenticar usuario (login).
 */
public interface AuthenticateUserUseCase {

    AuthResult authenticate(AuthenticateCommand command);

    record AuthenticateCommand(
            @NotBlank String usernameOrEmail,
            @NotBlank String rawPassword,
            String  ipAddress,
            String  userAgent
    ) {}
}
