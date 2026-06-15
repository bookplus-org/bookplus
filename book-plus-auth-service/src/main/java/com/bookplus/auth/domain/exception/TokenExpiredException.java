package com.bookplus.auth.domain.exception;

public class TokenExpiredException extends DomainException {
    public TokenExpiredException(String tokenType) {
        super("%s token has expired".formatted(tokenType));
    }
}
