package com.bookplus.auth.domain.port.out;

import com.bookplus.auth.domain.model.RefreshToken;

/** Puerto de salida — persistir refresh token. */
public interface SaveRefreshTokenPort {
    RefreshToken save(RefreshToken token);
    void update(RefreshToken token);
}
