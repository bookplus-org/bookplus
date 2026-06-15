package com.bookplus.order.application.coupon;

import com.bookplus.order.adapter.out.persistence.entity.CouponEntity;
import com.bookplus.order.adapter.out.persistence.repository.CouponJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/** Valida cupones y calcula el descuento sobre un importe. */
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponJpaRepository repository;

    public CouponResult evaluate(String code, BigDecimal amount) {
        if (code == null || code.isBlank()) {
            return new CouponResult(false, null, BigDecimal.ZERO, scale(amount), null);
        }
        CouponEntity c = repository.findById(code.trim().toUpperCase()).orElse(null);
        if (c == null || !c.isActive()) {
            return invalid("Cupón no válido", amount);
        }
        if (c.getExpiresAt() != null && c.getExpiresAt().isBefore(Instant.now())) {
            return invalid("El cupón ha expirado", amount);
        }
        if (c.getMinAmount() != null && amount.compareTo(c.getMinAmount()) < 0) {
            return invalid("Requiere una compra mínima de " + c.getMinAmount(), amount);
        }

        BigDecimal discount = "PERCENT".equalsIgnoreCase(c.getDiscountType())
                ? amount.multiply(c.getDiscountValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : c.getDiscountValue();
        if (discount.compareTo(amount) > 0) {
            discount = amount;
        }
        discount = scale(discount);
        return new CouponResult(true, c.getCode(), discount, scale(amount.subtract(discount)), "Cupón aplicado");
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
