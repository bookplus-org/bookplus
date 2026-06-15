package com.bookplus.auth.domain.exception;

public class TokenInvalidException extends DomainException {
    public TokenInvalidException(String tokenType) {
        super("%s token is invalid or has been revoked".formatted(tokenType));
    }
}
