package com.bookplus.auth.domain.port.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Puerto de entrada — Caso de uso: resetear contraseña con token.
 */
public interface ResetPasswordUseCase {

    void reset(ResetPasswordCommand command);

    record ResetPasswordCommand(
            @NotBlank                       String token,
            @NotBlank @Size(min = 8, max = 100) String newPassword
    ) {}
}
