package com.bookplus.auth.domain.port.out;

import com.bookplus.auth.domain.model.RefreshToken;
import com.bookplus.auth.domain.model.UserId;

import java.util.Optional;

/** Puerto de salida — consultas de refresh tokens. */
public interface LoadRefreshTokenPort {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    void revokeAllByUserId(UserId userId);
}
