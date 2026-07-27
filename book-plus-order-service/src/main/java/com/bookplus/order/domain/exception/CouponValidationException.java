package com.bookplus.order.domain.exception;

/** Datos de cupón inválidos (código en blanco, descuento no positivo, porcentaje > 100, etc.). */
public class CouponValidationException extends DomainException {
    public CouponValidationException(String message) {
        super(message);
    }
}
