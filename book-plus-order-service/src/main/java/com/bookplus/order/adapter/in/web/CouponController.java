package com.bookplus.order.adapter.in.web;

import com.bookplus.order.application.coupon.CouponService;
import com.bookplus.order.application.coupon.CouponService.CouponResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/** Validación de cupones desde el checkout. Ruta gateway: /api/v1/orders/**. */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Coupons", description = "Validate discount coupons")
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/validate-coupon")
    @Operation(summary = "Validate a coupon against an amount and return the discount")
    public ResponseEntity<CouponResult> validate(@RequestBody ValidateCouponRequest req) {
        return ResponseEntity.ok(couponService.evaluate(req.code(), req.amount()));
    }

    public record ValidateCouponRequest(String code, @NotNull BigDecimal amount) {}
}
