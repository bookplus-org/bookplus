package com.bookplus.order.domain.port.out;

import com.bookplus.order.domain.model.Coupon;

import java.util.List;
import java.util.Optional;

/** Puerto de salida para cupones: aísla la capa de aplicación de la persistencia (JPA). */
public interface CouponPort {

    Optional<Coupon> findByCode(String code);

    /** Todos los cupones (para la gestión de administración). */
    List<Coupon> findAll();

    boolean existsByCode(String code);

    /** Persiste un cupón (insert o replace). Debe conservar el createdAt del cupón recibido. */
    void save(Coupon coupon);

    /** Elimina por código; devuelve true si existía. */
    boolean deleteByCode(String code);
}
