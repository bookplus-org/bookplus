package com.bookplus.order.domain.port.out;

import com.bookplus.order.domain.model.Coupon;

import java.util.Optional;

/** Puerto de salida para cupones: aísla la capa de aplicación de la persistencia (JPA). */
public interface CouponPort {

    Optional<Coupon> findByCode(String code);

    /** Persiste un cupón nuevo (p. ej. un crédito en tienda emitido). */
    void save(Coupon coupon);
}
