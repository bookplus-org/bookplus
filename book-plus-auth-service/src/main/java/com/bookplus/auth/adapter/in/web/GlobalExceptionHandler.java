package com.bookplus.auth.adapter.in.web;

import com.bookplus.auth.domain.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manejador global de excepciones — RFC 7807 Problem Details.
 * Convierte excepciones de dominio en respuestas HTTP estandarizadas.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ProblemDetail handleUserAlreadyExists(UserAlreadyExistsException ex, WebRequest request) {
        return buildProblem(HttpStatus.CONFLICT, "User Already Exists", ex.getMessage(), request);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException ex, WebRequest request) {
        return buildProblem(HttpStatus.NOT_FOUND, "User Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex, WebRequest request) {
        return buildProblem(HttpStatus.UNAUTHORIZED, "Invalid Credentials", ex.getMessage(), request);
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ProblemDetail handleTokenExpired(TokenExpiredException ex, WebRequest request) {
        return buildProblem(HttpStatus.UNAUTHORIZED, "Token Expired", ex.getMessage(), request);
    }

    @ExceptionHandler(TokenInvalidException.class)
    public ProblemDetail handleTokenInvalid(TokenInvalidException ex, WebRequest request) {
        return buildProblem(HttpStatus.UNAUTHORIZED, "Token Invalid", ex.getMessage(), request);
    }

    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomainException(DomainException ex, WebRequest request) {
        log.warn("Domain exception: {}", ex.getMessage());
        return buildProblem(HttpStatus.BAD_REQUEST, "Business Rule Violation", ex.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        return buildProblem(HttpStatus.FORBIDDEN, "Access Denied",
                "You don't have permission to perform this action", request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {

        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        (e1, e2) -> e1));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, "Validation failed");
        problem.setTitle("Validation Error");
        problem.setType(URI.create("https://bookplus.com/errors/validation"));
        problem.setProperty("errors", errors);
        problem.setProperty("timestamp", Instant.now());

        return ResponseEntity.unprocessableEntity().body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, WebRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return buildProblem(HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.", request);
    }

    private ProblemDetail buildProblem(HttpStatus status, String title,
                                       String detail, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("https://bookplus.com/errors/" +
                title.toLowerCase().replace(" ", "-")));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
