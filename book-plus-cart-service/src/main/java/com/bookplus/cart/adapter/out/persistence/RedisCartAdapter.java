package com.bookplus.cart.adapter.out.persistence;

import com.bookplus.cart.domain.model.*;
import com.bookplus.cart.domain.port.out.*;
import com.bookplus.cart.shared.annotation.PersistenceAdapter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.*;

/**
 * Redis adapter — both LoadCartPort and SaveCartPort.
 *
 * Storage strategy:
 *  - Key  : "cart:{userId}"   (human-readable, supports O(1) lookup by userId)
 *  - Value: JSON-serialized CartRedisDto
 *  - TTL  : configurable (default 30 days) — extended on every write
 */
@PersistenceAdapter
@RequiredArgsConstructor
@Slf4j
public class RedisCartAdapter implements LoadCartPort, SaveCartPort {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper                  objectMapper;

    @Value("${cart.redis.ttl-days:30}")
    private long ttlDays;

    // ── Key helpers ───────────────────────────────────────────────────────

    private static String userKey(String userId) {
        return "cart:" + userId;
    }

    private static String idKey(String cartId) {
        return "cart:id:" + cartId;
    }

    // ── LoadCartPort ──────────────────────────────────────────────────────

    @Override
    public Optional<Cart> findByUserId(String userId) {
        return load(userKey(userId));
    }

    @Override
    public Optional<Cart> findById(CartId cartId) {
        // Secondary index: id → userId → actual cart
        String userId = redisTemplate.opsForValue().get(idKey(cartId.toString()));
        if (userId == null) return Optional.empty();
        return load(userKey(userId));
    }

    // ── SaveCartPort ──────────────────────────────────────────────────────

    @Override
    public Cart save(Cart cart) {
        try {
            CartRedisDto dto  = CartRedisDto.from(cart);
            String       json = objectMapper.writeValueAsString(dto);
            Duration     ttl  = Duration.ofDays(ttlDays);

            redisTemplate.opsForValue().set(userKey(cart.getUserId()), json, ttl);
            // Secondary index so findById also works
            redisTemplate.opsForValue().set(idKey(cart.getId().toString()), cart.getUserId(), ttl);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Failed to serialize cart to Redis", ex);
        }
        return cart;
    }

    @Override
    public void delete(String userId) {
        Optional<Cart> cart = findByUserId(userId);
        cart.ifPresent(c -> redisTemplate.delete(idKey(c.getId().toString())));
        redisTemplate.delete(userKey(userId));
    }

    // ── Internal deserialization ──────────────────────────────────────────

    private Optional<Cart> load(String key) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) return Optional.empty();
        try {
            CartRedisDto dto = objectMapper.readValue(json, CartRedisDto.class);
            return Optional.of(dto.toDomain());
        } catch (JsonProcessingException ex) {
            log.error("Failed to deserialize cart from Redis key {}: {}", key, ex.getMessage());
            return Optional.empty();
        }
    }
}
