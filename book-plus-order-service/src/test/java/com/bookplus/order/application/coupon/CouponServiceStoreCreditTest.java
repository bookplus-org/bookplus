package com.bookplus.order.application.coupon;

import com.bookplus.order.domain.model.Coupon;
import com.bookplus.order.domain.port.out.CouponPort;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CouponService.issueStoreCredit (crédito en tienda)")
class CouponServiceStoreCreditTest {

    @Mock private CouponPort couponPort;

    @InjectMocks private CouponService service;

    @Test
    @DisplayName("emite un cupón FIXED por el importe y devuelve un código CREDIT-…")
    void issuesFixedCoupon() {
        String code = service.issueStoreCredit(new BigDecimal("59.98"));

        assertThat(code).startsWith("CREDIT-");
        ArgumentCaptor<Coupon> captor = ArgumentCaptor.forClass(Coupon.class);
        then(couponPort).should().save(captor.capture());
        Coupon saved = captor.getValue();
        assertThat(saved.discountType()).isEqualTo("FIXED");
        assertThat(saved.discountValue()).isEqualByComparingTo("59.98");
        assertThat(saved.active()).isTrue();
        assertThat(saved.expiresAt()).isNotNull();
        assertThat(saved.code()).isEqualTo(code);
    }

    @Test
    @DisplayName("el crédito emitido es válido y canjeable por evaluate()")
    void issuedCreditIsRedeemable() {
        ArgumentCaptor<Coupon> captor = ArgumentCaptor.forClass(Coupon.class);

        String code = service.issueStoreCredit(new BigDecimal("20.00"));
        then(couponPort).should().save(captor.capture());
        given(couponPort.findByCode(code)).willReturn(Optional.of(captor.getValue()));

        CouponService.CouponResult r = service.evaluate(code, new BigDecimal("50.00"));
        assertThat(r.valid()).isTrue();
        assertThat(r.discount()).isEqualByComparingTo("20.00");
        assertThat(r.finalAmount()).isEqualByComparingTo("30.00");
    }

    @Test
    @DisplayName("importe no positivo lanza IllegalArgumentException")
    void nonPositive_throws() {
        assertThatThrownBy(() -> service.issueStoreCredit(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.issueStoreCredit(new BigDecimal("-5")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
