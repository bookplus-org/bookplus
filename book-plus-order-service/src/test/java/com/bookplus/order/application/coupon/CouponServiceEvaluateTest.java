package com.bookplus.order.application.coupon;

import com.bookplus.order.application.coupon.CouponService.CouponResult;
import com.bookplus.order.domain.model.Coupon;
import com.bookplus.order.domain.port.out.CouponPort;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * Cobertura exhaustiva de CouponService.evaluate(): todas las ramas de validación
 * y cálculo. Escrito a partir de los mutantes supervivientes que reportó PIT.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CouponService.evaluate (todas las ramas)")
class CouponServiceEvaluateTest {

    @Mock private CouponPort couponPort;

    private CouponService service;

    @BeforeEach
    void setUp() { service = new CouponService(couponPort); }

    private Coupon coupon(String type, BigDecimal value, BigDecimal minAmount, boolean active, Instant expiresAt) {
        return new Coupon("SAVE10", type, value, minAmount, active, expiresAt);
    }

    @Test
    @DisplayName("código nulo o en blanco → no válido, descuento cero")
    void blankCode() {
        CouponResult r = service.evaluate("  ", new BigDecimal("50.00"));
        assertThat(r.valid()).isFalse();
        assertThat(r.discount()).isEqualByComparingTo("0.00");
        assertThat(r.finalAmount()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("cupón inexistente → no válido")
    void notFound() {
        given(couponPort.findByCode("SAVE10")).willReturn(Optional.empty());
        CouponResult r = service.evaluate("save10", new BigDecimal("50.00"));
        assertThat(r.valid()).isFalse();
        assertThat(r.message()).contains("no válido");
    }

    @Test
    @DisplayName("cupón inactivo → no válido")
    void inactive() {
        given(couponPort.findByCode("SAVE10"))
                .willReturn(Optional.of(coupon("FIXED", new BigDecimal("10"), null, false, null)));
        assertThat(service.evaluate("SAVE10", new BigDecimal("50.00")).valid()).isFalse();
    }

    @Test
    @DisplayName("cupón expirado → no válido")
    void expired() {
        given(couponPort.findByCode("SAVE10")).willReturn(Optional.of(
                coupon("FIXED", new BigDecimal("10"), null, true, Instant.now().minus(1, ChronoUnit.DAYS))));
        CouponResult r = service.evaluate("SAVE10", new BigDecimal("50.00"));
        assertThat(r.valid()).isFalse();
        assertThat(r.message()).contains("expirado");
    }

    @Test
    @DisplayName("importe por debajo del mínimo → no válido")
    void belowMinimum() {
        given(couponPort.findByCode("SAVE10")).willReturn(Optional.of(
                coupon("FIXED", new BigDecimal("10"), new BigDecimal("100"), true, null)));
        CouponResult r = service.evaluate("SAVE10", new BigDecimal("50.00"));
        assertThat(r.valid()).isFalse();
        assertThat(r.message()).contains("mínima");
    }

    @Test
    @DisplayName("descuento PERCENT se calcula sobre el importe")
    void percentDiscount() {
        given(couponPort.findByCode("SAVE10")).willReturn(Optional.of(
                coupon("PERCENT", new BigDecimal("10"), null, true, null)));
        CouponResult r = service.evaluate("SAVE10", new BigDecimal("50.00"));
        assertThat(r.valid()).isTrue();
        assertThat(r.discount()).isEqualByComparingTo("5.00");      // 10% de 50
        assertThat(r.finalAmount()).isEqualByComparingTo("45.00");
        assertThat(r.message()).contains("aplicado");
    }

    @Test
    @DisplayName("descuento FIXED se aplica tal cual")
    void fixedDiscount() {
        given(couponPort.findByCode("SAVE10")).willReturn(Optional.of(
                coupon("FIXED", new BigDecimal("15"), null, true, null)));
        CouponResult r = service.evaluate("SAVE10", new BigDecimal("50.00"));
        assertThat(r.valid()).isTrue();
        assertThat(r.discount()).isEqualByComparingTo("15.00");
        assertThat(r.finalAmount()).isEqualByComparingTo("35.00");
    }

    @Test
    @DisplayName("el descuento nunca supera el importe (se topa)")
    void discountCappedAtAmount() {
        given(couponPort.findByCode("SAVE10")).willReturn(Optional.of(
                coupon("FIXED", new BigDecimal("80"), null, true, null)));
        CouponResult r = service.evaluate("SAVE10", new BigDecimal("50.00"));
        assertThat(r.valid()).isTrue();
        assertThat(r.discount()).isEqualByComparingTo("50.00");     // topado a 50, no 80
        assertThat(r.finalAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("cupón con fecha de expiración futura → válido")
    void notYetExpired() {
        given(couponPort.findByCode("SAVE10")).willReturn(Optional.of(
                coupon("FIXED", new BigDecimal("10"), null, true, Instant.now().plus(1, ChronoUnit.DAYS))));
        assertThat(service.evaluate("SAVE10", new BigDecimal("50.00")).valid()).isTrue();
    }
}
