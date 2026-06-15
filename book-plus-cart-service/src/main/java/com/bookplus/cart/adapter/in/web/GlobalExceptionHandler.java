package com.bookplus.cart.adapter.in.web;

import com.bookplus.cart.domain.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String BASE = "https://bookplus.com/errors/";

    // ── 404 ───────────────────────────────────────────────────────────────

    @ExceptionHandler(CartNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleCartNotFound(CartNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "cart-not-found", "Cart Not Found", ex.getMessage());
    }

    // ── 422 — Domain invariant violations ────────────────────────────────

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemDetail> handleDomain(DomainException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "domain-rule-violation",
                "Business Rule Violation", ex.getMessage());
    }

    // ── 400 — Bean Validation ─────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid",
                        (a, b) -> a
                ));
        ResponseEntity<ProblemDetail> response = build(HttpStatus.BAD_REQUEST, "validation-error",
                "Validation Error", "Validation failed");
        response.getBody().setProperty("errors", errors);
        return response;
    }

    // ── 403 ───────────────────────────────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "forbidden", "Forbidden", "Access denied");
    }

    // ── 500 ───────────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error",
                "Internal Server Error", "An unexpected error occurred");
    }

    private ResponseEntity<ProblemDetail> build(HttpStatus status, String type, String title, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create(BASE + type));
        pd.setTitle(title);
        pd.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(status).body(pd);
    }
}
