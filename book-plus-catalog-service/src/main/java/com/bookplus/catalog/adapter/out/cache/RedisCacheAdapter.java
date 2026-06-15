package com.bookplus.catalog.adapter.out.cache;

import com.bookplus.catalog.domain.model.Book;
import com.bookplus.catalog.domain.port.out.CachePort;
import com.bookplus.catalog.shared.annotation.PersistenceAdapter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * Adapter OUT — Redis cache para el catálogo de libros.
 *
 * Estrategia: cache-aside con TTL de 30 minutos.
 * Keys: "catalog:book:{id}", "catalog:book:isbn:{isbn}", "catalog:book:slug:{slug}"
 */
@PersistenceAdapter
@RequiredArgsConstructor
@Slf4j
public class RedisCacheAdapter implements CachePort {

    private static final Duration TTL          = Duration.ofMinutes(30);
    private static final String   CACHE_PREFIX = "catalog:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper                   objectMapper;

    @Override
    public Optional<Book> getBook(String key) {
        try {
            String json = redisTemplate.opsForValue().get(CACHE_PREFIX + key);
            if (json == null) return Optional.empty();
            return Optional.of(objectMapper.readValue(json, Book.class));
        } catch (Exception ex) {
            log.warn("Redis GET failed for key '{}': {}", key, ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void putBook(String key, Book book) {
        try {
            String json = objectMapper.writeValueAsString(book);
            redisTemplate.opsForValue().set(CACHE_PREFIX + key, json, TTL);
        } catch (JsonProcessingException ex) {
            log.warn("Redis SET failed for key '{}': {}", key, ex.getMessage());
        }
    }

    @Override
    public void evictBook(String key) {
        try {
            redisTemplate.delete(CACHE_PREFIX + key);
        } catch (Exception ex) {
            log.warn("Redis DELETE failed for key '{}': {}", key, ex.getMessage());
        }
    }

    @Override
    public void evictByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(CACHE_PREFIX + pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Evicted {} keys matching pattern '{}'", keys.size(), pattern);
            }
        } catch (Exception ex) {
            log.warn("Redis pattern eviction failed for '{}': {}", pattern, ex.getMessage());
        }
    }
}
