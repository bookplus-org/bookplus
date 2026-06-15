package com.bookplus.auth.domain.port.in;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Puerto de entrada — Caso de uso: registrar un nuevo usuario.
 * El Command es un record auto-validante co-localizado con el use case.
 */
public interface RegisterUserUseCase {

    AuthResult register(RegisterUserCommand command);

    record RegisterUserCommand(
            @NotBlank @Size(min = 3, max = 50) String username,
            @NotBlank @Email                   String email,
            @NotBlank @Size(min = 8, max = 100) String rawPassword
    ) {}
}
