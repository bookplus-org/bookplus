package com.bookplus.order.domain.port.in;

import com.bookplus.order.domain.model.Coupon;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Caso de uso de gestión de cupones (administración).
 * Contiene las reglas de negocio (normalización de código, validación de descuentos,
 * duplicados) para que el controlador solo traduzca HTTP.
 */
public interface ManageCouponUseCase {

    /** Todos los cupones, ordenados del más reciente al más antiguo. */
    List<Coupon> listAll();

    /** Crea un cupón nuevo. Lanza DuplicateCouponException / CouponValidationException. */
    Coupon create(CreateCouponCommand command);

    /** Activa o desactiva un cupón. Lanza CouponNotFoundException si no existe. */
    Coupon setActive(String code, boolean active);

    /** Elimina un cupón. Lanza CouponNotFoundException si no existe. */
    void delete(String code);

    record CreateCouponCommand(
            String     code,
            String     discountType,   // PERCENT | FIXED
            BigDecimal discountValue,
            BigDecimal minAmount,
            Instant    expiresAt
    ) {}
}
