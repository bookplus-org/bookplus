package com.bookplus.auth.domain.port.out;

import com.bookplus.auth.domain.model.Email;
import com.bookplus.auth.domain.model.User;
import com.bookplus.auth.domain.model.UserId;

import java.util.List;
import java.util.Optional;

/** Puerto de salida — consultas de usuarios. */
public interface LoadUserPort {
    Optional<User> findById(UserId id);
    Optional<User> findByEmail(Email email);
    Optional<User> findByUsername(String username);
    Optional<User> findByUsernameOrEmail(String usernameOrEmail);
    boolean existsByEmail(Email email);
    boolean existsByUsername(String username);

    /** Todos los usuarios, más recientes primero (uso administrativo). */
    List<User> findAll();
}
