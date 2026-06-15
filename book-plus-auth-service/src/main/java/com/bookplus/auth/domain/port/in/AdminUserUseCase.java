package com.bookplus.auth.domain.port.in;

import com.bookplus.auth.domain.model.User;
import com.bookplus.auth.domain.model.UserId;
import com.bookplus.auth.domain.model.UserRole;

import java.util.List;

/** Operaciones administrativas sobre usuarios (solo ADMIN / SUPERADMIN). */
public interface AdminUserUseCase {

    List<User> listUsers();

    /** Crea una cuenta desde el panel admin con un rol inicial. */
    User createUser(String username, String email, String rawPassword, UserRole role);

    /** Edita el perfil básico (nombre de usuario y correo). */
    User updateProfile(UserId id, String username, String email);

    /** Restablece la contraseña a un valor dado. */
    void resetPassword(UserId id, String newRawPassword);

    /** Activa o desactiva una cuenta. */
    User setEnabled(UserId id, boolean enabled, String reason);

    /** Otorga (grant=true) o revoca (grant=false) un rol. */
    User changeRole(UserId id, UserRole role, boolean grant);
}
