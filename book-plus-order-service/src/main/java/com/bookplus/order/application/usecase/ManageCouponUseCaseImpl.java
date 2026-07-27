package com.bookplus.order.application.usecase;

import com.bookplus.order.domain.exception.CouponNotFoundException;
import com.bookplus.order.domain.exception.CouponValidationException;
import com.bookplus.order.domain.exception.DuplicateCouponException;
import com.bookplus.order.domain.model.Coupon;
import com.bookplus.order.domain.port.in.ManageCouponUseCase;
import com.bookplus.order.domain.port.out.CouponPort;
import com.bookplus.order.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Reglas de negocio de gestión de cupones. Depende solo del puerto de salida {@link CouponPort},
 * nunca de JPA, de modo que el controlador queda libre de lógica de persistencia.
 */
@UseCase
@RequiredArgsConstructor
public class ManageCouponUseCaseImpl implements ManageCouponUseCase {

    private static final BigDecimal MAX_PERCENT = BigDecimal.valueOf(100);

    private final CouponPort couponPort;

    @Override
    public List<Coupon> listAll() {
        return couponPort.findAll().stream()
                .sorted(Comparator.comparing(Coupon::createdAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    public Coupon create(CreateCouponCommand cmd) {
        String code = normalize(cmd.code());
        if (code.isBlank()) {
            throw new CouponValidationException("El código es obligatorio");
        }
        String type = "FIXED".equalsIgnoreCase(cmd.discountType()) ? "FIXED" : "PERCENT";
        if (cmd.discountValue() == null || cmd.discountValue().signum() <= 0) {
            throw new CouponValidationException("El descuento debe ser mayor a 0");
        }
        if ("PERCENT".equals(type) && cmd.discountValue().compareTo(MAX_PERCENT) > 0) {
            throw new CouponValidationException("El porcentaje no puede superar 100");
        }
        if (couponPort.existsByCode(code)) {
            throw new DuplicateCouponException(code);
        }

        Coupon coupon = new Coupon(code, type, cmd.discountValue(), cmd.minAmount(),
                true, cmd.expiresAt(), Instant.now());
        couponPort.save(coupon);
        return coupon;
    }

    @Override
    public Coupon setActive(String code, boolean active) {
        String id = normalize(code);
        Coupon current = couponPort.findByCode(id)
                .orElseThrow(() -> new CouponNotFoundException(id));
        Coupon updated = current.withActive(active);
        couponPort.save(updated);
        return updated;
    }

    @Override
    public void delete(String code) {
        String id = normalize(code);
        if (!couponPort.deleteByCode(id)) {
            throw new CouponNotFoundException(id);
        }
    }

    private static String normalize(String code) {
        return code == null ? "" : code.trim().toUpperCase();
    }
}
