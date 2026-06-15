package com.bookplus.auth.domain.port.in;

import com.bookplus.auth.domain.model.User;
import com.bookplus.auth.domain.model.UserId;

/** Operaciones de autogestión de la cuenta del propio usuario autenticado. */
public interface AccountUseCase {

    User updateMyProfile(UserId id, String username, String email);

    /** Cambia la contraseña verificando primero la actual. */
    void changeMyPassword(UserId id, String currentPassword, String newPassword);
}
