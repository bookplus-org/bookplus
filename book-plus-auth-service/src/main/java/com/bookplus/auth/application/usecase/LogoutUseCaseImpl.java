package com.bookplus.auth.application.usecase;

import com.bookplus.auth.domain.model.RefreshToken;
import com.bookplus.auth.domain.port.in.LogoutUseCase;
import com.bookplus.auth.domain.port.out.LoadRefreshTokenPort;
import com.bookplus.auth.domain.port.out.SaveRefreshTokenPort;
import com.bookplus.auth.shared.annotation.UseCase;
import com.bookplus.auth.shared.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del caso de uso: Logout.
 * Revoca el refresh token y añade el access token al blacklist de Redis.
 */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class LogoutUseCaseImpl implements LogoutUseCase {

    private final LoadRefreshTokenPort loadRefreshTokenPort;
    private final SaveRefreshTokenPort saveRefreshTokenPort;
    private final JwtService           jwtService;

    @Override
    @Transactional
    public void logout(LogoutCommand command) {
        // Revocar refresh token
        String tokenHash = jwtService.hashToken(command.refreshToken());
        loadRefreshTokenPort.findByTokenHash(tokenHash)
                .ifPresent(token -> {
                    token.revoke();
                    saveRefreshTokenPort.update(token);
                    log.debug("Refresh token revoked for userId: {}", token.getUserId());
                });

        // Añadir access token a blacklist (Redis TTL = tiempo restante del token)
        jwtService.blacklistToken(command.accessToken());

        log.info("Logout completed");
    }
}
