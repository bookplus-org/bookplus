package com.bookplus.auth.domain.port.out;

import com.bookplus.auth.domain.model.PasswordResetToken;

import java.util.Optional;

/** Puerto de salida — consultas de tokens de reseteo. */
public interface LoadPasswordResetTokenPort {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
}
