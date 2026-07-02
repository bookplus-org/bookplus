package com.bookplus.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.security.Principal;

/**
 * Rate limiting avanzado del borde.
 *
 * Spring Cloud Gateway aplica {@code RequestRateLimiter} (token-bucket sobre Redis) por ruta.
 * La clave del límite la decide este {@link KeyResolver}: por defecto SCG usa el nombre del
 * principal, lo que deja al tráfico ANÓNIMO sin acotar. Aquí keyeamos por:
 *   1) usuario autenticado (nombre del principal) si existe, o
 *   2) IP del cliente (cabecera X-Forwarded-For si viene de un proxy, si no la IP remota).
 *
 * Así cada identidad real tiene su cubo de tokens y el tráfico anónimo también queda limitado
 * por IP (anti-abuso / anti-scraping), que es el comportamiento esperado en una API de banca.
 */
@Configuration
public class RateLimitConfig {

    @Bean
    public KeyResolver userOrIpKeyResolver() {
        return exchange -> exchange.getPrincipal()
                .map(Principal::getName)
                .switchIfEmpty(Mono.fromSupplier(() -> clientIp(exchange)));
    }

    private static String clientIp(org.springframework.web.server.ServerWebExchange exchange) {
        String xff = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();   // primera IP de la cadena de proxies
        }
        var addr = exchange.getRequest().getRemoteAddress();
        return addr != null && addr.getAddress() != null
                ? addr.getAddress().getHostAddress()
                : "unknown";
    }
}
