package com.bookplus.auth.domain.model;

import com.bookplus.auth.domain.event.DomainEvent;
import com.bookplus.auth.domain.event.UserDeactivatedEvent;
import com.bookplus.auth.domain.event.UserRegisteredEvent;
import com.bookplus.auth.domain.exception.DomainException;

import java.time.Instant;
import java.util.*;

/**
 * Aggregate Root — User.
 *
 * Encapsula toda la lógica de negocio relacionada con el usuario.
 * Sin anotaciones de frameworks. Puramente Java.
 *
 * Patrón: los domain events se acumulan y luego se publican
 * desde la capa de aplicación (use case) mediante el puerto DomainEventPublisherPort.
 */
public class User {

    private final UserId id;
    private String username;
    private Email email;
    private String passwordHash;
    private Set<UserRole> roles;
    private boolean enabled;
    private boolean emailVerified;
    private final Instant createdAt;
    private Instant updatedAt;

    // Domain events acumulados (no persistidos, solo para publicar)
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    // ── Constructor privado para factory methods ──────────────────────────

    private User(UserId id,
                 String username,
                 Email email,
                 String passwordHash,
                 Set<UserRole> roles,
                 boolean enabled,
                 boolean emailVerified,
                 Instant createdAt,
                 Instant updatedAt) {
        this.id            = Objects.requireNonNull(id, "id must not be null");
        this.username      = Objects.requireNonNull(username, "username must not be null");
        this.email         = Objects.requireNonNull(email, "email must not be null");
        this.passwordHash  = Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        this.roles         = new HashSet<>(Objects.requireNonNull(roles, "roles must not be null"));
        this.enabled       = enabled;
        this.emailVerified = emailVerified;
        this.createdAt     = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt     = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    // ── Factory Methods ───────────────────────────────────────────────────

    /**
     * Crea un nuevo usuario y registra el evento UserRegisteredEvent.
     */
    public static User create(String username, Email email, String passwordHash) {
        validateUsername(username);

        User user = new User(
                UserId.generate(),
                username.trim(),
                email,
                passwordHash,
                Set.of(UserRole.ROLE_USER),
                true,
                false,
                Instant.now(),
                Instant.now()
        );

        user.registerEvent(new UserRegisteredEvent(user.id, user.email, user.username));
        return user;
    }

    /**
     * Reconstituye un usuario existente desde la persistencia.
     * No genera eventos de dominio.
     */
    public static User reconstitute(UserId id,
                                    String username,
                                    Email email,
                                    String passwordHash,
                                    Set<UserRole> roles,
                                    boolean enabled,
                                    boolean emailVerified,
                                    Instant createdAt,
                                    Instant updatedAt) {
        return new User(id, username, email, passwordHash, roles,
                enabled, emailVerified, createdAt, updatedAt);
    }

    // ── Comportamientos de Dominio ────────────────────────────────────────

    public void verifyEmail() {
        if (this.emailVerified) {
            throw new DomainException("Email is already verified for user: " + username);
        }
        this.emailVerified = true;
        this.updatedAt = Instant.now();
    }

    public void changePassword(String newPasswordHash) {
        Objects.requireNonNull(newPasswordHash, "newPasswordHash must not be null");
        this.passwordHash = newPasswordHash;
        this.updatedAt = Instant.now();
    }

    /** Actualiza el perfil básico (nombre de usuario y correo). */
    public void changeProfile(String username, Email email) {
        validateUsername(username);
        this.username  = username.trim();
        this.email     = Objects.requireNonNull(email, "email must not be null");
        this.updatedAt = Instant.now();
    }

    public void deactivate(String reason) {
        if (!this.enabled) {
            throw new DomainException("User is already deactivated: " + username);
        }
        this.enabled = false;
        this.updatedAt = Instant.now();
        registerEvent(new UserDeactivatedEvent(this.id, this.email, reason));
    }

    public void activate() {
        this.enabled = true;
        this.updatedAt = Instant.now();
    }

    public void assignRole(UserRole role) {
        Objects.requireNonNull(role, "role must not be null");
        this.roles.add(role);
        this.updatedAt = Instant.now();
    }

    public void removeRole(UserRole role) {
        Objects.requireNonNull(role, "role must not be null");
        if (role == UserRole.ROLE_USER) {
            throw new DomainException("Cannot remove base role ROLE_USER from user");
        }
        this.roles.remove(role);
        this.updatedAt = Instant.now();
    }

    public boolean hasRole(UserRole role) {
        return roles.contains(role);
    }

    public boolean hasAtLeastRole(UserRole minRole) {
        return roles.stream().anyMatch(r -> r.isAtLeast(minRole));
    }

    // ── Domain Events ─────────────────────────────────────────────────────

    private void registerEvent(DomainEvent event) {
        domainEvents.add(event);
    }

    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return Collections.unmodifiableList(events);
    }

    // ── Validaciones ──────────────────────────────────────────────────────

    private static void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new DomainException("Username must not be blank");
        }
        if (username.trim().length() < 3 || username.trim().length() > 50) {
            throw new DomainException("Username must be between 3 and 50 characters");
        }
        if (!username.trim().matches("^[a-zA-Z0-9._-]+$")) {
            throw new DomainException("Username can only contain letters, digits, dots, underscores and hyphens");
        }
    }

    // ── Getters (sin setters — inmutabilidad controlada por métodos de dominio) ─

    public UserId getId()           { return id; }
    public String getUsername()     { return username; }
    public Email getEmail()         { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Set<UserRole> getRoles() { return Collections.unmodifiableSet(roles); }
    public boolean isEnabled()      { return enabled; }
    public boolean isEmailVerified(){ return emailVerified; }
    public Instant getCreatedAt()   { return createdAt; }
    public Instant getUpdatedAt()   { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User u)) return false;
        return id.equals(u.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "User{id=%s, username='%s', email='%s', enabled=%s}"
                .formatted(id, username, email, enabled);
    }
}
