package com.bookplus.auth.application.usecase;

import com.bookplus.auth.domain.exception.TokenExpiredException;
import com.bookplus.auth.domain.exception.TokenInvalidException;
import com.bookplus.auth.domain.exception.UserNotFoundException;
import com.bookplus.auth.domain.model.RefreshToken;
import com.bookplus.auth.domain.model.User;
import com.bookplus.auth.domain.port.in.AuthResult;
import com.bookplus.auth.domain.port.in.RefreshTokenUseCase;
import com.bookplus.auth.domain.port.out.*;
import com.bookplus.auth.shared.annotation.UseCase;
import com.bookplus.auth.shared.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del caso de uso: Refresh Token con rotación.
 *
 * El refresh token viejo se revoca y se genera uno nuevo.
 * Esto detecta reuso de tokens robados (si el token ya fue revocado, alerta de brecha).
 */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenUseCaseImpl implements RefreshTokenUseCase {

    private final LoadUserPort         loadUserPort;
    private final LoadRefreshTokenPort loadRefreshTokenPort;
    private final SaveRefreshTokenPort saveRefreshTokenPort;
    private final JwtService           jwtService;

    @Override
    @Transactional
    public AuthResult refresh(RefreshCommand command) {
        String tokenHash = jwtService.hashToken(command.refreshToken());

        RefreshToken storedToken = loadRefreshTokenPort.findByTokenHash(tokenHash)
                .orElseThrow(() -> new TokenInvalidException("Refresh"));

        // Detección de reutilización de token robado
        if (storedToken.isRevoked()) {
            log.warn("SECURITY ALERT: Refresh token reuse detected for userId: {}",
                    storedToken.getUserId());
            // Revocar todos los tokens del usuario como medida de seguridad
            loadRefreshTokenPort.revokeAllByUserId(storedToken.getUserId());
            throw new TokenInvalidException("Refresh");
        }

        if (storedToken.isExpired()) {
            throw new TokenExpiredException("Refresh");
        }

        // Cargar usuario
        User user = loadUserPort.findById(storedToken.getUserId())
                .orElseThrow(() -> new UserNotFoundException(storedToken.getUserId().toString()));

        if (!user.isEnabled()) {
            throw new TokenInvalidException("Refresh");
        }

        // Rotación: revocar token viejo
        storedToken.revoke();
        saveRefreshTokenPort.update(storedToken);

        // Emitir nuevos tokens
        String newAccessToken  = jwtService.generateAccessToken(user);
        String newRefreshValue = jwtService.generateRefreshTokenValue();
        var newRefreshToken = jwtService.buildRefreshToken(
                user, newRefreshValue, command.ipAddress(), command.userAgent());
        saveRefreshTokenPort.save(newRefreshToken);

        log.debug("Tokens rotated for user: {}", user.getUsername());

        return new AuthResult(
                newAccessToken,
                newRefreshValue,
                jwtService.getAccessTokenExpirationSeconds(),
                user.getId().toString(),
                user.getUsername(),
                user.getEmail().value(),
                user.getRoles(),
                user.isEmailVerified()
        );
    }
}
