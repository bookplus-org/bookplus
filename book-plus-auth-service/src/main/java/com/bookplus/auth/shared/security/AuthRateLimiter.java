package com.bookplus.auth.shared.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter por token bucket (Bucket4j), en memoria y por clave de cliente (IP+ruta).
 *
 * Cada clave tiene su propio "cubo" con una capacidad que se rellena de forma continua.
 * Mientras quedan fichas, las peticiones pasan; al agotarse, se rechazan hasta el
 * siguiente relleno. Protege endpoints sensibles (login, recuperación de contraseña)
 * frente a fuerza bruta y abuso. Por defecto: {@code capacity} peticiones cada
 * {@code period}.
 */
@Component
public class AuthRateLimiter {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final int capacity;
    private final Duration period;

    public AuthRateLimiter(
            @Value("${security.rate-limit.capacity:10}") int capacity,
            @Value("${security.rate-limit.period-seconds:60}") long periodSeconds) {
        this.capacity = capacity;
        this.period = Duration.ofSeconds(periodSeconds);
    }

    /** Intenta consumir una ficha para la clave dada. true = permitido, false = limitado. */
    public boolean tryConsume(String clientKey) {
        return buckets.computeIfAbsent(clientKey, k -> newBucket()).tryConsume(1);
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(capacity, period)
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}
