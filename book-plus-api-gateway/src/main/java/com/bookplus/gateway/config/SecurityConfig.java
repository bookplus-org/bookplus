package com.bookplus.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Reactive security for Spring Cloud Gateway (WebFlux).
 *
 * Strategy:
 *  - Public routes (catalog read, auth, actuator, swagger) → permitAll
 *  - Everything else → requires valid JWT
 *  - The gateway validates JWT once and forwards downstream;
 *    each service also validates independently for defence-in-depth.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${gateway.jwt.public-key-base64}")
    private String publicKeyBase64;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                // ── Public ──────────────────────────────────────────────
                .pathMatchers("/api/v1/auth/**").permitAll()
                // Catalog reads are public
                .pathMatchers("GET", "/api/v1/books/**").permitAll()
                .pathMatchers("GET", "/api/v1/categories/**").permitAll()
                // Actuator health — public
                .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                // Swagger UIs per service
                .pathMatchers("/*/v3/api-docs/**", "/*/swagger-ui/**").permitAll()
                // Payment webhook (called by gateway, not by users)
                .pathMatchers("POST", "/api/v1/payments/webhook").permitAll()
                // SSE de pedidos: EventSource no envía Authorization; el JWT viaja
                // como ?token=… y lo valida order-service. Por eso es público aquí.
                .pathMatchers("GET", "/api/v1/orders/stream").permitAll()
                // ── Authenticated ────────────────────────────────────────
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtDecoder(jwtDecoder()))
            );

        return http.build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        try {
            byte[]       decoded = Base64.getDecoder().decode(publicKeyBase64);
            RSAPublicKey key     = (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(decoded));
            return NimbusReactiveJwtDecoder.withPublicKey(key).build();
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot load RSA public key for api-gateway", ex);
        }
    }
}
