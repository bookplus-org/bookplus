package com.bookplus.auth.domain.port.out;

import com.bookplus.auth.domain.model.PasswordResetToken;

/** Puerto de salida — persistir token de reseteo. */
public interface SavePasswordResetTokenPort {
    PasswordResetToken save(PasswordResetToken token);
    void update(PasswordResetToken token);
}
