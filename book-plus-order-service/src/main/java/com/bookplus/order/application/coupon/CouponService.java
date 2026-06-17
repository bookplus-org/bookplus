package com.bookplus.order.application.coupon;

import com.bookplus.order.domain.model.Coupon;
import com.bookplus.order.domain.port.out.CouponPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/** Valida cupones y calcula el descuento sobre un importe. Depende del puerto, no de JPA. */
@Service
@RequiredArgsConstructor
public class CouponService {

    /** Validez por defecto del crédito en tienda emitido como alternativa al reembolso. */
    private static final long STORE_CREDIT_VALID_DAYS = 365;

    private final CouponPort couponPort;

    /**
     * Emite un crédito en tienda como alternativa al reembolso en efectivo: crea un cupón
     * FIXED por el importe indicado y devuelve su código. Se usa cuando la política decide
     * STORE_CREDIT (p. ej. un libro digital ya consumido dentro de la ventana).
     */
    public String issueStoreCredit(BigDecimal amount) {
        BigDecimal value = scale(amount == null ? BigDecimal.ZERO : amount);
        if (value.signum() <= 0) {
            throw new IllegalArgumentException("El importe del crédito debe ser positivo");
        }
        String code = "CREDIT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        couponPort.save(new Coupon(code, "FIXED", value, null, true,
                Instant.now().plus(STORE_CREDIT_VALID_DAYS, ChronoUnit.DAYS)));
        return code;
    }

    public CouponResult evaluate(String code, BigDecimal amount) {
        if (code == null || code.isBlank()) {
            return new CouponResult(false, null, BigDecimal.ZERO, scale(amount), null);
        }
        Coupon c = couponPort.findByCode(code.trim().toUpperCase()).orElse(null);
        if (c == null || !c.active()) {
            return invalid("Cupón no válido", amount);
        }
        if (c.isExpired(Instant.now())) {
            return invalid("El cupón ha expirado", amount);
        }
        if (c.minAmount() != null && amount.compareTo(c.minAmount()) < 0) {
            return invalid("Requiere una compra mínima de " + c.minAmount(), amount);
        }

        BigDecimal discount = c.isPercent()
                ? amount.multiply(c.discountValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : c.discountValue();
        if (discount.compareTo(amount) > 0) {
            discount = amount;
        }
        discount = scale(discount);
        return new CouponResult(true, c.code(), discount, scale(amount.subtract(discount)), "Cupón aplicado");
    }

    private CouponResult invalid(String message, BigDecimal amount) {
        return new CouponResult(false, null, BigDecimal.ZERO, scale(amount), message);
    }

    private static BigDecimal scale(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    public record CouponResult(
            boolean valid, String code, BigDecimal discount, BigDecimal finalAmount, String message) {}
}
