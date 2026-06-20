package com.bookplus.auth.shared.security.lockout;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Bloqueo de cuenta tras varios intentos de login fallidos.
 *
 * Complementa al rate limiting por IP con un control por CUENTA: aunque un atacante use miles
 * de IPs distintas (fuerza bruta distribuida), tras N fallos sobre la misma cuenta esta se
 * bloquea temporalmente. Los contadores viven en Redis con TTL, así que la ventana y el
 * bloqueo caducan solos.
 */
@Component
public class LoginAttemptService {

    private final LoginAttemptStore store;
    private final int maxAttempts;
    private final Duration attemptWindow;
    private final Duration lockDuration;

    public LoginAttemptService(
            LoginAttemptStore store,
            @Value("${security.lockout.max-attempts:5}") int maxAttempts,
            @Value("${security.lockout.window-seconds:900}") long windowSeconds,
            @Value("${security.lockout.lock-seconds:900}") long lockSeconds) {
        this.store = store;
        this.maxAttempts = maxAttempts;
        this.attemptWindow = Duration.ofSeconds(windowSeconds);
        this.lockDuration = Duration.ofSeconds(lockSeconds);
    }

    public boolean isLocked(String usernameOrEmail) {
        return store.exists(lockKey(usernameOrEmail));
    }

    /** Registra un intento fallido; al alcanzar el máximo, bloquea la cuenta. */
    public void recordFailure(String usernameOrEmail) {
        long attempts = store.incrementAndGet(attemptsKey(usernameOrEmail), attemptWindow);
        if (attempts >= maxAttempts) {
            store.put(lockKey(usernameOrEmail), "1", lockDuration);
            store.evict(attemptsKey(usernameOrEmail));
        }
    }

    /** Login correcto: limpia contadores y cualquier bloqueo. */
    public void recordSuccess(String usernameOrEmail) {
        store.evict(attemptsKey(usernameOrEmail), lockKey(usernameOrEmail));
    }

    private String attemptsKey(String id) { return "login:attempts:" + norm(id); }
    private String lockKey(String id)     { return "login:locked:"   + norm(id); }
    private String norm(String id)        { return id == null ? "" : id.trim().toLowerCase(); }
}
