package com.bookplus.payment.adapter.in.web;

import com.bookplus.payment.adapter.in.web.dto.*;
import com.bookplus.payment.domain.model.Payment;
import com.bookplus.payment.domain.port.in.*;
import com.bookplus.payment.shared.annotation.WebAdapter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@WebAdapter
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment processing")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final GetPaymentUseCase     getPaymentUseCase;
    private final ProcessPaymentUseCase processPaymentUseCase;
    private final RefundPaymentUseCase  refundPaymentUseCase;

    // ── GET /api/v1/payments/{paymentId} ──────────────────────────────────

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment by ID")
    public ResponseEntity<PaymentResponse> getById(
            @PathVariable String paymentId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Payment payment = getPaymentUseCase.getByPaymentId(paymentId);
        // Users can only see their own payments; admins see any
        if (!payment.getUserId().equals(jwt.getSubject()) && !isAdmin(jwt))
            return ResponseEntity.status(403).build();
        return ResponseEntity.ok(PaymentResponse.from(payment));
    }

    // ── GET /api/v1/payments/order/{orderId} ──────────────────────────────

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get payment by order ID")
    public ResponseEntity<PaymentResponse> getByOrder(
            @PathVariable String orderId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Payment payment = getPaymentUseCase.getByOrderId(orderId);
        if (!payment.getUserId().equals(jwt.getSubject()) && !isAdmin(jwt))
            return ResponseEntity.status(403).build();
        return ResponseEntity.ok(PaymentResponse.from(payment));
    }

    // ── POST /api/v1/payments/webhook — gateway callback ─────────────────

    @PostMapping("/webhook")
    @Operation(summary = "Payment gateway webhook — marks payment completed or failed",
               description = "Called by the payment gateway. In production, validate the webhook signature.")
    public ResponseEntity<PaymentResponse> webhook(
            @Valid @RequestBody GatewayWebhookRequest req
    ) {
        Payment payment = switch (req.status().toLowerCase()) {
            case "completed" -> processPaymentUseCase.complete(req.paymentId(), req.transactionRef());
            case "failed"    -> processPaymentUseCase.fail(req.paymentId(), req.failureReason());
            default -> throw new IllegalArgumentException("Unknown webhook status: " + req.status());
        };
        return ResponseEntity.ok(PaymentResponse.from(payment));
    }

    // ── POST /api/v1/payments/order/{orderId}/refund — ADMIN ─────────────

    @PostMapping("/order/{orderId}/refund")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(summary = "[ADMIN] Initiate refund for an order")
    public ResponseEntity<PaymentResponse> refund(
            @PathVariable String orderId,
            @RequestParam(defaultValue = "Order cancelled") String reason
    ) {
        return ResponseEntity.ok(PaymentResponse.from(refundPaymentUseCase.refund(orderId, reason)));
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private boolean isAdmin(Jwt jwt) {
        Object roles = jwt.getClaim("roles");
        if (roles instanceof Iterable<?> list)
            for (Object r : list)
                if ("ADMIN".equals(r) || "SUPERADMIN".equals(r)) return true;
        return false;
    }
}
