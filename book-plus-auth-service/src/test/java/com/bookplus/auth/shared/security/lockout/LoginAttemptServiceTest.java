package com.bookplus.auth.shared.security.lockout;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica el bloqueo de cuenta con un store en memoria (sin Redis). Máximo = 3 intentos.
 */
class LoginAttemptServiceTest {

    /** Store en memoria (ignora TTL). */
    static class InMemoryStore implements LoginAttemptStore {
        final Map<String, Long> counters = new HashMap<>();
        final Set<String> keys = new HashSet<>();
        public long incrementAndGet(String k, Duration ttl) { long v = counters.merge(k, 1L, Long::sum); keys.add(k); return v; }
        public boolean exists(String k) { return keys.contains(k); }
        public void put(String k, String v, Duration ttl) { keys.add(k); }
        public void evict(String... ks) { for (String k : ks) { counters.remove(k); keys.remove(k); } }
    }

    private final LoginAttemptService service =
            new LoginAttemptService(new InMemoryStore(), 3, 900, 900);

    @Test
    void bloquea_la_cuenta_tras_alcanzar_el_maximo_de_fallos() {
        assertThat(service.isLocked("ada")).isFalse();

        service.recordFailure("ada");
        service.recordFailure("ada");
        assertThat(service.isLocked("ada")).isFalse();   // 2 fallos: aún no

        service.recordFailure("ada");
        assertThat(service.isLocked("ada")).isTrue();    // 3er fallo: bloqueada
    }

    @Test
    void un_login_correcto_resetea_los_intentos() {
        service.recordFailure("ada");
        service.recordFailure("ada");
        service.recordSuccess("ada");                    // login OK

        service.recordFailure("ada");
        assertThat(service.isLocked("ada")).isFalse();   // contador reiniciado
    }

    @Test
    void cuentas_distintas_son_independientes() {
        service.recordFailure("ada");
        service.recordFailure("ada");
        service.recordFailure("ada");                    // ada bloqueada
        assertThat(service.isLocked("ada")).isTrue();
        assertThat(service.isLocked("grace")).isFalse(); // grace no
    }
}
