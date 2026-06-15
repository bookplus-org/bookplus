package com.bookplus.auth.application.usecase;

import com.bookplus.auth.domain.model.Email;
import com.bookplus.auth.domain.model.PasswordResetToken;
import com.bookplus.auth.domain.port.in.ForgotPasswordUseCase;
import com.bookplus.auth.domain.port.out.*;
import com.bookplus.auth.shared.annotation.UseCase;
import com.bookplus.auth.shared.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Implementación del caso de uso: Solicitar Reset de Contraseña.
 *
 * Seguridad: SIEMPRE responde 200 OK sin revelar si el email existe (anti-enumeration).
 */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class ForgotPasswordUseCaseImpl implements ForgotPasswordUseCase {

    private final LoadUserPort              loadUserPort;
    private final SavePasswordResetTokenPort savePasswordResetTokenPort;
    private final EmailNotificationPort     emailNotification;
    private final JwtService                jwtService;

    @Value("${application.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    @Transactional
    public void requestReset(ForgotPasswordCommand command) {
        Email email = Email.of(command.email());

        // Buscar usuario — si no existe, terminamos silenciosamente (anti-enumeration)
        loadUserPort.findByEmail(email).ifPresent(user -> {
            // Generar token seguro de 32 bytes
            byte[] tokenBytes = new byte[32];
            SECURE_RANDOM.nextBytes(tokenBytes);
            String plainToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
            String tokenHash  = jwtService.hashToken(plainToken);

            PasswordResetToken resetToken = PasswordResetToken.create(user.getId(), tokenHash);
            savePasswordResetTokenPort.save(resetToken);

            String resetLink = "%s/reset-password?token=%s".formatted(frontendUrl, plainToken);

            try {
                emailNotification.sendPasswordResetEmail(
                        email.value(), user.getUsername(), resetLink);
                log.info("Password reset email sent to: {}", email);
            } catch (Exception ex) {
                log.error("Failed to send password reset email to {}: {}", email, ex.getMessage());
            }
        });

        // Siempre log genérico, sin revelar si existía el email
        log.debug("Password reset requested for email: {}", email);
    }
}
