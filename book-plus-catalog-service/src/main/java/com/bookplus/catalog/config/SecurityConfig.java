package com.bookplus.catalog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Seguridad del catalog-service.
 *
 * Estrategia: Resource Server OAuth2 / JWT (RS256).
 * El catalog-service valida el token JWT usando la clave pública del auth-service.
 *
 * Rutas públicas (GET): libros, categorías, búsqueda, reseñas
 * Rutas protegidas: POST reseñas (cualquier usuario autenticado)
 * Rutas admin: /api/v1/admin/** (EDITOR / ADMIN / SUPERADMIN)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${security.jwt.public-key-base64:}")
    private String publicKeyBase64;

    // ── JWT Decoder (RS256) ───────────────────────────────────────────────

    @Bean
    public JwtDecoder jwtDecoder() {
        if (publicKeyBase64 == null || publicKeyBase64.isBlank()) {
            // Dev mode: use auth-service JWKS endpoint via issuer-uri (spring auto-config)
            // This bean won't override spring.security.oauth2.resourceserver.jwt.issuer-uri
            throw new IllegalStateException(
                    "JWT public key not configured. " +
                    "Set CATALOG_JWT_PUBLIC_KEY_BASE64 or configure issuer-uri.");
        }
        try {
            byte[] decoded = java.util.Base64.getDecoder().decode(publicKeyBase64);
            java.security.KeyFactory kf = java.security.KeyFactory.getInstance("RSA");
            java.security.spec.X509EncodedKeySpec spec =
                    new java.security.spec.X509EncodedKeySpec(decoded);
            java.security.interfaces.RSAPublicKey publicKey =
                    (java.security.interfaces.RSAPublicKey) kf.generatePublic(spec);
            return NimbusJwtDecoder.withPublicKey(publicKey).build();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load JWT public key", ex);
        }
    }

    // ── Roles claim converter ─────────────────────────────────────────────

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedConverter = new JwtGrantedAuthoritiesConverter();
        grantedConverter.setAuthoritiesClaimName("roles");
        // El claim "roles" ya incluye el prefijo ROLE_ (p. ej. ROLE_SUPERADMIN),
        // así que no debemos volver a anteponerlo (evita ROLE_ROLE_SUPERADMIN).
        grantedConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(grantedConverter);
        return converter;
    }

    // ── Security Filter Chain ─────────────────────────────────────────────

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public read endpoints
                        .requestMatchers(HttpMethod.GET, "/api/v1/books/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()
                        // OpenAPI / Actuator
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**",
                                         "/swagger-ui.html", "/actuator/health").permitAll()
                        // GraphQL — API de agregación de lectura (pública, como los GET REST)
                        .requestMatchers("/graphql", "/graphiql/**").permitAll()
                        // Admin
                        .requestMatchers("/api/v1/admin/**")
                            .hasAnyRole("EDITOR", "ADMIN", "SUPERADMIN")
                        // Authenticated for POST reviews
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build();
    }

    // ── CORS ──────────────────────────────────────────────────────────────

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
