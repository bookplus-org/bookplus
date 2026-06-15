package com.bookplus.auth.adapter.out.verification;

import com.bookplus.auth.adapter.out.persistence.entity.EmailVerificationTokenEntity;
import com.bookplus.auth.adapter.out.persistence.repository.EmailVerificationTokenJpaRepository;
import com.bookplus.auth.domain.exception.DomainException;
import com.bookplus.auth.domain.model.User;
import com.bookplus.auth.domain.model.UserId;
import com.bookplus.auth.domain.port.out.EmailNotificationPort;
import com.bookplus.auth.domain.port.out.EmailVerificationPort;
import com.bookplus.auth.domain.port.out.LoadUserPort;
import com.bookplus.auth.domain.port.out.SaveUserPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationAdapter implements EmailVerificationPort {

    private static final Duration TTL = Duration.ofHours(24);

    private final EmailVerificationTokenJpaRepository tokenRepo;
    private final LoadUserPort          loadUserPort;
    private final SaveUserPort          saveUserPort;
    private final EmailNotificationPort emailNotification;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Override
    @Transactional
    public void createAndSendVerification(UUID userId, String email, String username) {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokenRepo.save(EmailVerificationTokenEntity.builder()
                .token(token)
                .userId(userId)
                .expiresAt(Instant.now().plus(TTL))
                .used(false)
                .createdAt(Instant.now())
                .build());

        String link = frontendUrl + "/auth/verify-email?token=" + token;
        emailNotification.sendVerificationEmail(email, username, link);
    }

    @Override
    @Transactional
    public void verifyToken(String token) {
        EmailVerificationTokenEntity t = tokenRepo.findById(token)
                .orElseThrow(() -> new DomainException("Enlace de verificación inválido"));
        if (t.isUsed()) {
            throw new DomainException("Este enlace ya fue utilizado");
        }
        if (t.getExpiresAt().isBefore(Instant.now())) {
            throw new DomainException("El enlace de verificación ha expirado");
        }

        User user = loadUserPort.findById(UserId.of(t.getUserId()))
                .orElseThrow(() -> new DomainException("Usuario no encontrado"));
        if (!user.isEmailVerified()) {
            user.verifyEmail();
            saveUserPort.save(user);
        }
        t.setUsed(true);
        tokenRepo.save(t);
        log.info("Email verified for user {}", t.getUserId());
    }
}
