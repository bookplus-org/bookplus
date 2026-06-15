package com.bookplus.auth.domain.port.out;

import java.util.UUID;

/** Puerto de salida — verificación de correo. */
public interface EmailVerificationPort {

    /** Genera un token y envía el correo de verificación. */
    void createAndSendVerification(UUID userId, String email, String username);

    /** Valida el token y marca el correo del usuario como verificado. */
    void verifyToken(String token);
}
