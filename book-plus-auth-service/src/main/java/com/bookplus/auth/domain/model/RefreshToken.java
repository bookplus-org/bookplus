package com.bookplus.auth.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity — Refresh Token de sesión.
 * Pertenece al aggregate User pero se persiste por separado.
 */
public class RefreshToken {

    private final UUID id;
    private final UserId userId;
    private final String tokenHash;   // almacenamos hash, no el token en claro
    private final Instant expiresAt;
    private final String ipAddress;
    private final String userAgent;
    private boolean revoked;
    private final Instant createdAt;

    private RefreshToken(UUID id, UserId userId, String tokenHash,
                         Instant expiresAt, String ipAddress, String userAgent,
                         boolean revoked, Instant createdAt) {
        this.id        = Objects.requireNonNull(id);
        this.userId    = Objects.requireNonNull(userId);
        this.tokenHash = Objects.requireNonNull(tokenHash);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.revoked   = revoked;
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public static RefreshToken create(UserId userId, String tokenHash,
                                      Instant expiresAt, String ipAddress, String userAgent) {
        return new RefreshToken(UUID.randomUUID(), userId, tokenHash,
                expiresAt, ipAddress, userAgent, false, Instant.now());
    }

    public static RefreshToken reconstitute(UUID id, UserId userId, String tokenHash,
                                            Instant expiresAt, String ipAddress, String userAgent,
                                            boolean revoked, Instant createdAt) {
        return new RefreshToken(id, userId, tokenHash, expiresAt,
                ipAddress, userAgent, revoked, createdAt);
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !revoked && !isExpired();
    }

    public void revoke() {
        this.revoked = true;
    }

    public UUID getId()           { return id; }
    public UserId getUserId()     { return userId; }
    public String getTokenHash()  { return tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public String getIpAddress()  { return ipAddress; }
    public String getUserAgent()  { return userAgent; }
    public boolean isRevoked()    { return revoked; }
    public Instant getCreatedAt() { return createdAt; }
}
