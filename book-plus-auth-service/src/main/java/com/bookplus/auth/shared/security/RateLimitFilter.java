package com.bookplus.auth.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Filtro de rate limiting para los endpoints de autenticación sensibles.
 *
 * Limita por IP + ruta usando {@link AuthRateLimiter}. Al exceder el límite responde
 * 429 (Too Many Requests) con cabecera Retry-After, sin llegar al controller. Se ejecuta
 * antes del filtro JWT.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> LIMITED_PATHS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password");

    private final AuthRateLimiter rateLimiter;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !("POST".equalsIgnoreCase(request.getMethod())
                && LIMITED_PATHS.contains(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String clientKey = clientIp(request) + ":" + request.getRequestURI();

        if (rateLimiter.tryConsume(clientKey)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit excedido para {} en {}", clientIp(request), request.getRequestURI());
            response.setStatus(429); // Too Many Requests
            response.setContentType("application/json");
            response.setHeader("Retry-After", "60");
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Demasiadas peticiones. Inténtalo de nuevo en un momento.\"}");
        }
    }

    /** Respeta X-Forwarded-For (detrás del gateway) y cae al remote addr si no está. */
    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
