package com.bookplus.auth.application.usecase;

import com.bookplus.auth.domain.exception.InvalidCredentialsException;
import com.bookplus.auth.domain.model.User;
import com.bookplus.auth.domain.port.in.AuthResult;
import com.bookplus.auth.domain.port.in.AuthenticateUserUseCase;
import com.bookplus.auth.domain.port.out.*;
import com.bookplus.auth.shared.annotation.UseCase;
import com.bookplus.auth.shared.security.JwtService;
import com.bookplus.auth.shared.security.lockout.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del caso de uso: Autenticar Usuario (Login).
 */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class AuthenticateUserUseCaseImpl implements AuthenticateUserUseCase {

    private final LoadUserPort         loadUserPort;
    private final SaveRefreshTokenPort saveRefreshTokenPort;
    private final PasswordEncoder      passwordEncoder;
    private final JwtService           jwtService;
    private final LoginAttemptService  loginAttempts;

    @Override
    @Transactional
    public AuthResult authenticate(AuthenticateCommand command) {
        log.debug("Authentication attempt for: {}", command.usernameOrEmail());

        // Cuenta bloqueada por demasiados intentos fallidos (control por cuenta).
        if (loginAttempts.isLocked(command.usernameOrEmail())) {
            log.warn("Login bloqueado temporalmente (demasiados fallos): {}", command.usernameOrEmail());
            throw new InvalidCredentialsException(); // no revelamos el motivo exacto
        }

        // Buscar usuario por username o email
        User user = loadUserPort.findByUsernameOrEmail(command.usernameOrEmail())
                .orElseThrow(InvalidCredentialsException::new);

        // Verificar contraseña
        if (!passwordEncoder.matches(command.rawPassword(), user.getPasswordHash())) {
            log.warn("Failed login attempt for user: {}", command.usernameOrEmail());
            loginAttempts.recordFailure(command.usernameOrEmail());
            throw new InvalidCredentialsException();
        }

        // Verificar cuenta activa
        if (!user.isEnabled()) {
            throw new InvalidCredentialsException(); // No revelamos que la cuenta está desactivada
        }

        // Login correcto: limpiar contador de fallos.
        loginAttempts.recordSuccess(command.usernameOrEmail());

        // Generar tokens
        String accessToken       = jwtService.generateAccessToken(user);
        String refreshTokenValue = jwtService.generateRefreshTokenValue();
        var refreshToken = jwtService.buildRefreshToken(
                user, refreshTokenValue, command.ipAddress(), command.userAgent());
        saveRefreshTokenPort.save(refreshToken);

        log.info("User '{}' authenticated successfully", user.getUsername());

        return new AuthResult(
                accessToken,
                refreshTokenValue,
                jwtService.getAccessTokenExpirationSeconds(),
                user.getId().toString(),
                user.getUsername(),
                user.getEmail().value(),
                user.getRoles(),
                user.isEmailVerified()
        );
    }
}
