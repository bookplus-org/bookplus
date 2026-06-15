package com.bookplus.catalog.adapter.in.web;

import com.bookplus.catalog.domain.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Adapter IN — Manejador global de excepciones (RFC 7807 ProblemDetail).
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String TYPE_BASE = "https://bookplus.com/errors/";

    // ── 404 Not Found ───────────────────────────────────────────────────────

    @ExceptionHandler({BookNotFoundException.class, CategoryNotFoundException.class})
    public ResponseEntity<ProblemDetail> handleNotFound(DomainException ex, WebRequest request) {
        ProblemDetail pd = buildProblem(HttpStatus.NOT_FOUND, "not-found",
                ex.getMessage(), request);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }

    // ── 409 Conflict ────────────────────────────────────────────────────────

    @ExceptionHandler({BookAlreadyExistsException.class, CategoryAlreadyExistsException.class})
    public ResponseEntity<ProblemDetail> handleConflict(DomainException ex, WebRequest request) {
        ProblemDetail pd = buildProblem(HttpStatus.CONFLICT, "already-exists",
                ex.getMessage(), request);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }

    // ── 422 Unprocessable Entity — general domain violations ────────────────

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemDetail> handleDomain(DomainException ex, WebRequest request) {
        ProblemDetail pd = buildProblem(HttpStatus.UNPROCESSABLE_ENTITY, "domain-error",
                ex.getMessage(), request);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(pd);
    }

    // ── 400 Bad Request — Bean Validation ───────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException ex, WebRequest request) {

        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid",
                        (a, b) -> a
                ));

        ProblemDetail pd = buildProblem(HttpStatus.BAD_REQUEST, "validation-error",
                "Request validation failed", request);
        pd.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(pd);
    }

    // ── 403 Forbidden ────────────────────────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(
            AccessDeniedException ex, WebRequest request) {
        ProblemDetail pd = buildProblem(HttpStatus.FORBIDDEN, "access-denied",
                "Access denied", request);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
    }

    // ── 500 Internal Server Error ────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneral(Exception ex, WebRequest request) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        ProblemDetail pd = buildProblem(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error",
                "An unexpected error occurred", request);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(pd);
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private ProblemDetail buildProblem(HttpStatus status, String errorType,
                                       String detail, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create(TYPE_BASE + errorType));
        pd.setProperty("timestamp", Instant.now());
        pd.setProperty("path", request.getDescription(false).replace("uri=", ""));
        return pd;
    }
}
