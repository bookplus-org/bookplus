package com.bookplus.auth.domain.port.in;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Puerto de entrada — Caso de uso: solicitar reset de contraseña.
 * Siempre responde 200 OK (no revela si el email existe).
 */
public interface ForgotPasswordUseCase {

    void requestReset(ForgotPasswordCommand command);

    record ForgotPasswordCommand(
            @NotBlank @Email String email
    ) {}
}
