package com.bookplus.inventory.adapter.in.web;

import com.bookplus.inventory.domain.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String TYPE_BASE = "https://bookplus.com/errors/";

    @ExceptionHandler(StockNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(StockNotFoundException ex, WebRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                build(HttpStatus.NOT_FOUND, "stock-not-found", ex.getMessage(), req));
    }

    @ExceptionHandler(ReservationNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(ReservationNotFoundException ex, WebRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                build(HttpStatus.NOT_FOUND, "reservation-not-found", ex.getMessage(), req));
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ProblemDetail> handleInsufficientStock(InsufficientStockException ex, WebRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                build(HttpStatus.CONFLICT, "insufficient-stock", ex.getMessage(), req));
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemDetail> handleDomain(DomainException ex, WebRequest req) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
                build(HttpStatus.UNPROCESSABLE_ENTITY, "domain-error", ex.getMessage(), req));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex, WebRequest req) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid",
                        (a, b) -> a));
        ProblemDetail pd = build(HttpStatus.BAD_REQUEST, "validation-error", "Validation failed", req);
        pd.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(pd);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex, WebRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                build(HttpStatus.FORBIDDEN, "access-denied", "Access denied", req));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneral(Exception ex, WebRequest req) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                build(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error", "An unexpected error occurred", req));
    }

    private ProblemDetail build(HttpStatus status, String type, String detail, WebRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create(TYPE_BASE + type));
        pd.setProperty("timestamp", Instant.now());
        pd.setProperty("path", req.getDescription(false).replace("uri=", ""));
        return pd;
    }
}
