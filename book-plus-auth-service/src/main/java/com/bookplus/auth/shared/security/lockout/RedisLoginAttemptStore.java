package com.bookplus.auth.shared.security.lockout;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;

/** Adaptador Redis del LoginAttemptStore (contadores con TTL nativo). */
@Component
public class RedisLoginAttemptStore implements LoginAttemptStore {

    private final StringRedisTemplate redis;

    public RedisLoginAttemptStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public long incrementAndGet(String key, Duration ttl) {
        Long value = redis.opsForValue().increment(key);
        if (value != null && value == 1L) {
            redis.expire(key, ttl);   // TTL solo en el primer fallo de la ventana
        }
        return value == null ? 0L : value;
    }

    @Override
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redis.hasKey(key));
    }

    @Override
    public void put(String key, String value, Duration ttl) {
        redis.opsForValue().set(key, value, ttl);
    }

    @Override
    public void evict(String... keys) {
        redis.delete(Arrays.asList(keys));
    }
}
