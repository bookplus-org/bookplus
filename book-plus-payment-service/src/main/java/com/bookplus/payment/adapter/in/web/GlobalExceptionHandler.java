package com.bookplus.payment.adapter.in.web;

import com.bookplus.payment.domain.exception.*;
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

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(PaymentNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "payment-not-found", "Payment Not Found", ex.getMessage());
    }

    @ExceptionHandler(PaymentAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handleConflict(PaymentAlreadyExistsException ex) {
        return build(HttpStatus.CONFLICT, "payment-conflict", "Payment Already Exists", ex.getMessage());
    }

    @ExceptionHandler({InvalidPaymentTransitionException.class, DomainException.class})
    public ResponseEntity<ProblemDetail> handleDomain(DomainException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "payment-rule-violation",
                "Payment Rule Violation", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid",
                        (a, b) -> a));
        ResponseEntity<ProblemDetail> response = build(HttpStatus.BAD_REQUEST, "validation-error",
                "Validation Error", "Validation failed");
        response.getBody().setProperty("errors", errors);
        return response;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccess(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "forbidden", "Forbidden", "Access denied");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(Exception ex) {
        log.error("Unhandled: {}", ex.getMessage(), ex);
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
