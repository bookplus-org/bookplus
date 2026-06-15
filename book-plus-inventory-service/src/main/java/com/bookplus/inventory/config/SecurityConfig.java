package com.bookplus.inventory.config;

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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${security.jwt.public-key-base64:}")
    private String publicKeyBase64;

    @Bean
    public JwtDecoder jwtDecoder() {
        if (publicKeyBase64 == null || publicKeyBase64.isBlank()) {
            throw new IllegalStateException("INVENTORY_JWT_PUBLIC_KEY_BASE64 must be set");
        }
        try {
            byte[] decoded = java.util.Base64.getDecoder().decode(publicKeyBase64);
            java.security.KeyFactory kf = java.security.KeyFactory.getInstance("RSA");
            java.security.interfaces.RSAPublicKey key =
                    (java.security.interfaces.RSAPublicKey) kf.generatePublic(
                            new java.security.spec.X509EncodedKeySpec(decoded));
            return NimbusJwtDecoder.withPublicKey(key).build();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load JWT public key", ex);
        }
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter gc = new JwtGrantedAuthoritiesConverter();
        gc.setAuthoritiesClaimName("roles");
        // El claim "roles" ya trae el prefijo ROLE_; no anteponerlo de nuevo.
        gc.setAuthorityPrefix("");
        JwtAuthenticationConverter conv = new JwtAuthenticationConverter();
        conv.setJwtGrantedAuthoritiesConverter(gc);
        return conv;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Stock actual — público (usado por catalog-service y API Gateway)
                        .requestMatchers(HttpMethod.GET, "/api/v1/inventory/*").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**",
                                         "/actuator/health").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build();
    }
}
