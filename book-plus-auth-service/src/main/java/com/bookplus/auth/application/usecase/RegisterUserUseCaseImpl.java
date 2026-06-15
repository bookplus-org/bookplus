package com.bookplus.auth.application.usecase;

import com.bookplus.auth.domain.model.Email;
import com.bookplus.auth.domain.model.User;
import com.bookplus.auth.domain.exception.UserAlreadyExistsException;
import com.bookplus.auth.domain.port.in.AuthResult;
import com.bookplus.auth.domain.port.in.RegisterUserUseCase;
import com.bookplus.auth.domain.port.out.*;
import com.bookplus.auth.shared.annotation.UseCase;
import com.bookplus.auth.shared.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del caso de uso: Registrar Usuario.
 *
 * Orquesta: validación → creación del agregado → persistencia → publicación de eventos.
 */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class RegisterUserUseCaseImpl implements RegisterUserUseCase {

    private final LoadUserPort          loadUserPort;
    private final SaveUserPort          saveUserPort;
    private final SaveRefreshTokenPort  saveRefreshTokenPort;
    private final DomainEventPublisherPort eventPublisher;
    private final EmailNotificationPort emailNotification;
    private final com.bookplus.auth.domain.port.out.EmailVerificationPort emailVerification;
    private final PasswordEncoder       passwordEncoder;
    private final JwtService            jwtService;

    @Override
    @Transactional
    public AuthResult register(RegisterUserCommand command) {
        log.info("Registering new user: {}", command.username());

        Email email = Email.of(command.email());

        // Validaciones de unicidad
        if (loadUserPort.existsByEmail(email)) {
            throw new UserAlreadyExistsException("email", command.email());
        }
        if (loadUserPort.existsByUsername(command.username())) {
            throw new UserAlreadyExistsException("username", command.username());
        }

        // Creación del agregado — lógica de dominio pura
        String passwordHash = passwordEncoder.encode(command.rawPassword());
        User user = User.create(command.username(), email, passwordHash);

        // Persistir
        User savedUser = saveUserPort.save(user);
        log.info("User persisted with id: {}", savedUser.getId());

        // Generar tokens JWT
        String accessToken  = jwtService.generateAccessToken(savedUser);
        String refreshTokenValue = jwtService.generateRefreshTokenValue();
        var refreshToken = jwtService.buildRefreshToken(savedUser, refreshTokenValue, null, null);
        saveRefreshTokenPort.save(refreshToken);

        // Publicar domain events acumulados
        var events = savedUser.pullDomainEvents();
        eventPublisher.publishAll(events);

        // Enviar email de bienvenida + verificación (sin bloquear el flujo)
        try {
            emailNotification.sendWelcomeEmail(email.value(), savedUser.getUsername());
        } catch (Exception ex) {
            log.warn("Failed to send welcome email to {}: {}", email, ex.getMessage());
        }
        try {
            emailVerification.createAndSendVerification(
                    savedUser.getId().value(), email.value(), savedUser.getUsername());
        } catch (Exception ex) {
            log.warn("Failed to send verification email to {}: {}", email, ex.getMessage());
        }

        log.info("User {} registered successfully", command.username());

        return new AuthResult(
                accessToken,
                refreshTokenValue,
                jwtService.getAccessTokenExpirationSeconds(),
                savedUser.getId().toString(),
                savedUser.getUsername(),
                savedUser.getEmail().value(),
                savedUser.getRoles(),
                savedUser.isEmailVerified()
        );
    }
}
