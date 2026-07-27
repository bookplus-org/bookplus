package com.bookplus.order.adapter.in.web;

import com.bookplus.order.domain.model.Coupon;
import com.bookplus.order.domain.port.in.ManageCouponUseCase;
import com.bookplus.order.domain.port.in.ManageCouponUseCase.CreateCouponCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Gestión de cupones para el admin.
 * Ruta gateway: /api/v1/orders/** (reaprovecha el route del order-service).
 *
 * Este controlador es un adaptador de entrada "delgado": solo traduce HTTP ⇄ comandos y
 * delega toda la lógica (validación, duplicados, persistencia) en {@link ManageCouponUseCase}.
 */
@RestController
@RequestMapping("/api/v1/orders/admin/coupons")
@RequiredArgsConstructor
@Tag(name = "Admin Coupons", description = "Create / list / toggle discount coupons")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
public class AdminCouponController {

    private final ManageCouponUseCase manageCouponUseCase;

    @GetMapping
    @Operation(summary = "List all coupons")
    public List<CouponResponse> list() {
        return manageCouponUseCase.listAll().stream()
                .map(CouponResponse::from)
                .toList();
    }

    @PostMapping
    @Operation(summary = "Create (or replace) a coupon")
    public ResponseEntity<CouponResponse> create(@RequestBody CreateCouponRequest req) {
        Coupon created = manageCouponUseCase.create(new CreateCouponCommand(
                req.code(), req.discountType(), req.discountValue(), req.minAmount(), req.expiresAt()));
        return ResponseEntity.status(HttpStatus.CREATED).body(CouponResponse.from(created));
    }

    @PatchMapping("/{code}/active")
    @Operation(summary = "Enable or disable a coupon")
    public CouponResponse setActive(@PathVariable String code, @RequestParam boolean value) {
        return CouponResponse.from(manageCouponUseCase.setActive(code, value));
    }

    @DeleteMapping("/{code}")
    @Operation(summary = "Delete a coupon")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        manageCouponUseCase.delete(code);
        return ResponseEntity.noContent().build();
    }

    // ── DTOs ──────────────────────────────────────────────────────────────

    public record CreateCouponRequest(
            @NotBlank String code,
            @NotBlank String discountType,        // PERCENT | FIXED
            @NotNull  BigDecimal discountValue,
            BigDecimal minAmount,
            Instant expiresAt
    ) {}

    public record CouponResponse(
            String code,
            String discountType,
            BigDecimal discountValue,
            BigDecimal minAmount,
            boolean active,
            Instant expiresAt,
            Instant createdAt
    ) {
        static CouponResponse from(Coupon c) {
            return new CouponResponse(
                    c.code(), c.discountType(), c.discountValue(),
                    c.minAmount(), c.active(), c.expiresAt(), c.createdAt());
        }
    }
}
