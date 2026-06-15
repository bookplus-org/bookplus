package com.bookplus.auth.domain.port.in;

import com.bookplus.auth.domain.model.UserRole;

import java.util.Set;

/**
 * Resultado compartido entre AuthenticateUserUseCase y RefreshTokenUseCase.
 * Contiene los tokens generados y datos básicos del usuario.
 */
public record AuthResult(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresIn,   // segundos
        String userId,
        String username,
        String email,
        Set<UserRole> roles,
        boolean emailVerified
) {}
