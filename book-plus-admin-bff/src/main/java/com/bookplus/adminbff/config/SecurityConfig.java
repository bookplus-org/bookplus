package com.bookplus.adminbff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.web.SecurityFilterChain;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Admin BFF is accessible only by ADMIN and SUPERADMIN roles.
 * Every endpoint (except actuator/swagger) requires authentication.
 */
@Configuration @EnableWebSecurity @EnableMethodSecurity
public class SecurityConfig {

    @Value("${admin-bff.jwt.public-key-base64}")
    private String publicKeyBase64;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**").permitAll()
                .anyRequest().hasAnyRole("ADMIN", "SUPERADMIN")
            )
            .oauth2ResourceServer(o -> o.jwt(j -> j
                .decoder(jwtDecoder())
                .jwtAuthenticationConverter(jwtAuthenticationConverter())
            ));
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        try {
            byte[] decoded = Base64.getDecoder().decode(publicKeyBase64);
            RSAPublicKey key = (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(decoded));
            return NimbusJwtDecoder.withPublicKey(key).build();
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot load RSA public key for admin-bff", ex);
        }
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter gac = new JwtGrantedAuthoritiesConverter();
        gac.setAuthoritiesClaimName("roles");
        // El claim "roles" ya incluye el prefijo ROLE_; no anteponerlo de nuevo.
        gac.setAuthorityPrefix("");
        JwtAuthenticationConverter conv = new JwtAuthenticationConverter();
        conv.setJwtGrantedAuthoritiesConverter(gac);
        return conv;
    }
}
