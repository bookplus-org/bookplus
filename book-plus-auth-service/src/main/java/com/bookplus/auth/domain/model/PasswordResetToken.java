package com.bookplus.auth.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity — Token para reseteo de contraseña.
 * Expira en 1 hora y es de un solo uso.
 */
public class PasswordResetToken {

    private final UUID id;
    private final UserId userId;
    private final String tokenHash;
    private final Instant expiresAt;
    private boolean used;
    private final Instant createdAt;

    private PasswordResetToken(UUID id, UserId userId, String tokenHash,
                               Instant expiresAt, boolean used, Instant createdAt) {
        this.id        = Objects.requireNonNull(id);
        this.userId    = Objects.requireNonNull(userId);
        this.tokenHash = Objects.requireNonNull(tokenHash);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        this.used      = used;
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public static PasswordResetToken create(UserId userId, String tokenHash) {
        return new PasswordResetToken(
                UUID.randomUUID(), userId, tokenHash,
                Instant.now().plusSeconds(3600), // 1 hora
                false, Instant.now()
        );
    }

    public static PasswordResetToken reconstitute(UUID id, UserId userId, String tokenHash,
                                                   Instant expiresAt, boolean used, Instant createdAt) {
        return new PasswordResetToken(id, userId, tokenHash, expiresAt, used, createdAt);
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !used && !isExpired();
    }

    public void markAsUsed() {
        this.used = true;
    }

    public UUID getId()           { return id; }
    public UserId getUserId()     { return userId; }
    public String getTokenHash()  { return tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isUsed()       { return used; }
    public Instant getCreatedAt() { return createdAt; }
}
