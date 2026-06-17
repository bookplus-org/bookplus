package com.bookplus.order.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Cupón de descuento (vista de dominio). El detalle de persistencia vive en el
 * adaptador; la capa de aplicación solo conoce este modelo a través de {@code CouponPort}.
 *
 * @param discountType "PERCENT" o "FIXED"
 */
public record Coupon(
        String     code,
        String     discountType,
        BigDecimal discountValue,
        BigDecimal minAmount,
        boolean    active,
        Instant    expiresAt
) {
    public boolean isPercent() { return "PERCENT".equalsIgnoreCase(discountType); }

    public boolean isExpired(Instant now) {
        return expiresAt != null && expiresAt.isBefore(now);
    }
}
