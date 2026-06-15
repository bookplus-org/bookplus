package com.bookplus.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global security headers filter.
 * Runs on every response before it is sent back to the client.
 *
 * Headers added:
 *  - X-Content-Type-Options: nosniff
 *      Prevents MIME-type sniffing attacks.
 *  - X-Frame-Options: DENY
 *      Prevents clickjacking via iframes.
 *  - X-XSS-Protection: 1; mode=block
 *      Legacy XSS protection (modern browsers rely on CSP).
 *  - Strict-Transport-Security
 *      Forces HTTPS for 1 year; enable includeSubDomains in production.
 *  - Referrer-Policy: strict-origin-when-cross-origin
 *      Controls how much referrer info is sent.
 *  - Content-Security-Policy
 *      Basic CSP — tighten for production to restrict allowed origins.
 *  - Permissions-Policy
 *      Disables dangerous browser features (camera, geolocation, etc.).
 */
@Component
public class SecurityHeadersFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            HttpHeaders headers = exchange.getResponse().getHeaders();

            headers.set("X-Content-Type-Options",   "nosniff");
            headers.set("X-Frame-Options",           "DENY");
            headers.set("X-XSS-Protection",          "1; mode=block");
            headers.set("Referrer-Policy",           "strict-origin-when-cross-origin");

            // HSTS — only effective over HTTPS; safe to include here
            headers.set("Strict-Transport-Security",
                    "max-age=31536000; includeSubDomains");

            // Permissions Policy — disable sensitive browser APIs
            headers.set("Permissions-Policy",
                    "camera=(), microphone=(), geolocation=(), payment=()");

            // Content Security Policy
            // default-src 'self' — only load resources from same origin
            // script-src includes 'unsafe-inline' for Swagger UI (remove in prod for stricter policy)
            headers.set("Content-Security-Policy",
                    "default-src 'self'; " +
                    "script-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com; " +
                    "style-src 'self' 'unsafe-inline'; " +
                    "img-src 'self' data: https:; " +
                    "connect-src 'self'; " +
                    "frame-ancestors 'none'");
        }));
    }

    @Override
    public int getOrder() {
        // Run after routing but before response is committed
        return Ordered.LOWEST_PRECEDENCE - 10;
    }
}
