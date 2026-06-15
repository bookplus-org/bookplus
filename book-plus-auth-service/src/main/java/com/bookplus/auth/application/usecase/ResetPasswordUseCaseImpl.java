package com.bookplus.auth.application.usecase;

import com.bookplus.auth.domain.exception.TokenExpiredException;
import com.bookplus.auth.domain.exception.TokenInvalidException;
import com.bookplus.auth.domain.exception.UserNotFoundException;
import com.bookplus.auth.domain.port.in.ResetPasswordUseCase;
import com.bookplus.auth.domain.port.out.*;
import com.bookplus.auth.shared.annotation.UseCase;
import com.bookplus.auth.shared.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del caso de uso: Resetear Contraseña.
 */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class ResetPasswordUseCaseImpl implements ResetPasswordUseCase {

    private final LoadUserPort               loadUserPort;
    private final SaveUserPort               saveUserPort;
    private final LoadPasswordResetTokenPort loadPasswordResetTokenPort;
    private final SavePasswordResetTokenPort savePasswordResetTokenPort;
    private final LoadRefreshTokenPort       loadRefreshTokenPort;
    private final PasswordEncoder            passwordEncoder;
    private final JwtService                 jwtService;

    @Override
    @Transactional
    public void reset(ResetPasswordCommand command) {
        String tokenHash = jwtService.hashToken(command.token());

        var resetToken = loadPasswordResetTokenPort.findByTokenHash(tokenHash)
                .orElseThrow(() -> new TokenInvalidException("Password reset"));

        if (resetToken.isExpired()) {
            throw new TokenExpiredException("Password reset");
        }
        if (!resetToken.isValid()) {
            throw new TokenInvalidException("Password reset");
        }

        var user = loadUserPort.findById(resetToken.getUserId())
                .orElseThrow(() -> new UserNotFoundException(resetToken.getUserId().toString()));

        // Cambiar contraseña en el agregado
        String newHash = passwordEncoder.encode(command.newPassword());
        user.changePassword(newHash);
        saveUserPort.save(user);

        // Marcar token como usado
        resetToken.markAsUsed();
        savePasswordResetTokenPort.update(resetToken);

        // Revocar todos los refresh tokens activos por seguridad
        loadRefreshTokenPort.revokeAllByUserId(user.getId());

        log.info("Password reset successful for userId: {}", user.getId());
    }
}
