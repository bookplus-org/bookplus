package com.bookplus.auth.shared.security.lockout;

import java.time.Duration;

/** Almacén de intentos/bloqueos de login (puerto). Producción: Redis. Tests: en memoria. */
public interface LoginAttemptStore {
    /** Incrementa el contador de la clave (fija TTL en el primer incremento) y devuelve el nuevo valor. */
    long incrementAndGet(String key, Duration ttl);
    boolean exists(String key);
    void put(String key, String value, Duration ttl);
    void evict(String... keys);
}
