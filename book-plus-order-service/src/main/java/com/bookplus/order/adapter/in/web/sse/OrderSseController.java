package com.bookplus.order.adapter.in.web.sse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Stream SSE de cambios de pedido para el usuario autenticado.
 * EventSource no envía el header Authorization, así que el JWT viaja como ?token=…
 * y se valida aquí con el mismo JwtDecoder del resource server.
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders SSE", description = "Real-time order updates via Server-Sent Events")
public class OrderSseController {

    private final OrderSseHub hub;
    private final JwtDecoder  jwtDecoder;

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Subscribe to real-time updates for the current user's orders")
    public SseEmitter stream(@RequestParam("token") String token) {
        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(token);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido");
        }
        return hub.subscribe(jwt.getSubject(), isStaff(jwt));
    }

    private boolean isStaff(Jwt jwt) {
        Object roles = jwt.getClaim("roles");
        if (roles instanceof Iterable<?> list)
            for (Object r : list)
                if ("ROLE_ADMIN".equals(r) || "ROLE_SUPERADMIN".equals(r) || "ROLE_REPARTIDOR".equals(r))
                    return true;
        return false;
    }
}
