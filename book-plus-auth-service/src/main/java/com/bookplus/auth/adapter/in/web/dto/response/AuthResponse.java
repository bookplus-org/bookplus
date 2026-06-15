package com.bookplus.auth.adapter.in.web.dto.response;

import com.bookplus.auth.domain.model.UserRole;
import com.bookplus.auth.domain.port.in.AuthResult;

import java.util.Set;

public record AuthResponse(
        String      accessToken,
        String      refreshToken,
        String      tokenType,
        long        expiresIn,
        String      userId,
        String      username,
        String      email,
        Set<UserRole> roles,
        boolean     emailVerified
) {
    public static AuthResponse from(AuthResult result) {
        return new AuthResponse(
                result.accessToken(),
                result.refreshToken(),
                "Bearer",
                result.accessTokenExpiresIn(),
                result.userId(),
                result.username(),
                result.email(),
                result.roles(),
                result.emailVerified()
        );
    }
}
