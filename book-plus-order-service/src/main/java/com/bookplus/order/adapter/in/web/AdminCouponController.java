package com.bookplus.order.adapter.in.web;

import com.bookplus.order.adapter.out.persistence.entity.CouponEntity;
import com.bookplus.order.adapter.out.persistence.repository.CouponJpaRepository;
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
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Gestión de cupones para el admin.
 * Ruta gateway: /api/v1/orders/** (reaprovecha el route del order-service).
 */
@RestController
@RequestMapping("/api/v1/orders/admin/coupons")
@RequiredArgsConstructor
@Tag(name = "Admin Coupons", description = "Create / list / toggle discount coupons")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
public class AdminCouponController {

    private final CouponJpaRepository repository;

    @GetMapping
    @Operation(summary = "List all coupons")
    public List<CouponResponse> list() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(CouponEntity::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(CouponResponse::from)
                .toList();
    }

    @PostMapping
    @Operation(summary = "Create (or replace) a coupon")
    public ResponseEntity<CouponResponse> create(@RequestBody CreateCouponRequest req) {
        String code = req.code() == null ? "" : req.code().trim().toUpperCase();
        if (code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código es obligatorio");
        }
        if (repository.existsById(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un cupón con ese código");
        }
        String type = "FIXED".equalsIgnoreCase(req.discountType()) ? "FIXED" : "PERCENT";
        if (req.discountValue() == null || req.discountValue().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El descuento debe ser mayor a 0");
        }
        if ("PERCENT".equals(type) && req.discountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El porcentaje no puede superar 100");
        }

        CouponEntity saved = repository.save(CouponEntity.builder()
                .code(code)
                .discountType(type)
                .discountValue(req.discountValue())
                .minAmount(req.minAmount())
                .expiresAt(req.expiresAt())
                .active(true)
                .createdAt(Instant.now())
                .build());
        return ResponseEntity.status(HttpStatus.CREATED).body(CouponResponse.from(saved));
    }

    @PatchMapping("/{code}/active")
    @Operation(summary = "Enable or disable a coupon")
    public CouponResponse setActive(@PathVariable String code, @RequestParam boolean value) {
        CouponEntity c = repository.findById(code.trim().toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cupón no encontrado"));
        c.setActive(value);
        return CouponResponse.from(repository.save(c));
    }

    @DeleteMapping("/{code}")
    @Operation(summary = "Delete a coupon")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        String id = code.trim().toUpperCase();
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cupón no encontrado");
        }
        repository.deleteById(id);
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
        static CouponResponse from(CouponEntity c) {
            return new CouponResponse(
                    c.getCode(), c.getDiscountType(), c.getDiscountValue(),
                    c.getMinAmount(), c.isActive(), c.getExpiresAt(), c.getCreatedAt());
        }
    }
}
