package com.bookplus.auth.shared.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica el comportamiento del token bucket: permite hasta la capacidad y luego limita,
 * y trata cada clave de cliente de forma independiente.
 */
class AuthRateLimiterTest {

    @Test
    void permite_hasta_la_capacidad_y_luego_bloquea() {
        AuthRateLimiter limiter = new AuthRateLimiter(3, 60);
        String key = "1.2.3.4:/api/v1/auth/login";

        assertThat(limiter.tryConsume(key)).isTrue();   // 1
        assertThat(limiter.tryConsume(key)).isTrue();   // 2
        assertThat(limiter.tryConsume(key)).isTrue();   // 3
        assertThat(limiter.tryConsume(key)).isFalse();  // 4 -> limitado
    }

    @Test
    void claves_distintas_son_independientes() {
        AuthRateLimiter limiter = new AuthRateLimiter(1, 60);

        assertThat(limiter.tryConsume("ip-A")).isTrue();
        assertThat(limiter.tryConsume("ip-A")).isFalse();   // A agotada
        assertThat(limiter.tryConsume("ip-B")).isTrue();    // B tiene su propio cubo
    }
}
