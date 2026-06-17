package com.bookplus.order.adapter.in.web;

import com.bookplus.order.adapter.in.web.dto.*;
import com.bookplus.order.application.refund.RefundDecisionService;
import com.bookplus.order.domain.model.Order;
import com.bookplus.order.domain.model.OrderStatus;
import com.bookplus.order.domain.policy.RefundContext;
import com.bookplus.order.domain.port.in.*;
import com.bookplus.order.shared.annotation.WebAdapter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@WebAdapter
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order management")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final GetOrderUseCase           getOrderUseCase;
    private final CancelOrderUseCase        cancelOrderUseCase;
    private final UpdateOrderStatusUseCase  updateStatusUseCase;
    private final RefundDecisionService     refundDecisionService;

    // ── GET /api/v1/orders — list current user's orders ───────────────────

    @GetMapping
    @Operation(summary = "List current user's orders (paginated)")
    public ResponseEntity<PagedOrderResponse> listMyOrders(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        String userId = jwt.getSubject();
        List<OrderResponse> orders = getOrderUseCase.getByUserId(userId, page, size)
                .stream().map(o -> OrderResponse.from(o, true)).toList();
        long total = getOrderUseCase.countByUserId(userId);
        return ResponseEntity.ok(PagedOrderResponse.of(orders, page, size, total));
    }

    // ── GET /api/v1/orders/{orderId} ──────────────────────────────────────

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order by ID — accessible by owner or admin")
    public ResponseEntity<OrderResponse> getOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String orderId
    ) {
        Order order = getOrderUseCase.getById(orderId);
        boolean owner = order.getUserId().equals(jwt.getSubject());
        // Users can only see their own orders; admins see any
        if (!owner && !isAdmin(jwt))
            return ResponseEntity.status(403).build();
        // El código de entrega solo se muestra al dueño (para dárselo al repartidor).
        return ResponseEntity.ok(OrderResponse.from(order, owner));
    }

    // ── POST /api/v1/orders/{orderId}/confirm-receipt — el cliente confirma ─
    @PostMapping("/{orderId}/confirm-receipt")
    @Operation(summary = "Customer confirms they received the order")
    public ResponseEntity<OrderResponse> confirmReceipt(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String orderId
    ) {
        Order updated = updateStatusUseCase.confirmReceipt(orderId, jwt.getSubject());
        return ResponseEntity.ok(OrderResponse.from(updated, true));
    }

    // ── POST /api/v1/orders/{orderId}/claim — el cliente reclama ──────────
    @PostMapping("/{orderId}/claim")
    @Operation(summary = "Customer opens a claim (e.g. 'order not received')")
    public ResponseEntity<OrderResponse> openClaim(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String orderId,
            @Valid @RequestBody ClaimRequest req
    ) {
        Order updated = updateStatusUseCase.openClaim(orderId, jwt.getSubject(), req.reason());
        return ResponseEntity.ok(OrderResponse.from(updated, true));
    }

    @PatchMapping("/{orderId}/claim/resolve")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(summary = "[ADMIN] Resolve a claim with a note")
    public ResponseEntity<OrderResponse> resolveClaim(
            @PathVariable String orderId,
            @Valid @RequestBody ResolveClaimRequest req
    ) {
        return ResponseEntity.ok(OrderResponse.from(
                updateStatusUseCase.resolveClaim(orderId, req.resolution())));
    }

    // ── DELETE /api/v1/orders/{orderId} — cancel ──────────────────────────

    @DeleteMapping("/{orderId}")
    @Operation(summary = "Cancel an order (user can cancel own orders in cancellable states)")
    public ResponseEntity<OrderResponse> cancelOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String orderId,
            @Valid @RequestBody CancelOrderRequest req
    ) {
        Order order = cancelOrderUseCase.cancel(orderId, jwt.getSubject(), req.reason());
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    // ── Admin — listar todos los pedidos ──────────────────────────────────

    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(summary = "[ADMIN] List all orders (optional status filter)")
    public ResponseEntity<PagedOrderResponse> listAll(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        OrderStatus filter = (status == null || status.isBlank())
                ? null : OrderStatus.valueOf(status.toUpperCase());
        List<OrderResponse> orders = getOrderUseCase.getAll(filter, page, size)
                .stream().map(OrderResponse::from).toList();
        long total = getOrderUseCase.countAll(filter);
        return ResponseEntity.ok(PagedOrderResponse.of(orders, page, size, total));
    }

    // ── Admin — status transitions ────────────────────────────────────────

    @PatchMapping("/{orderId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(summary = "[ADMIN] Cancel any order")
    public ResponseEntity<OrderResponse> adminCancel(
            @PathVariable String orderId,
            @Valid @RequestBody CancelOrderRequest req
    ) {
        return ResponseEntity.ok(OrderResponse.from(
                cancelOrderUseCase.cancelAsAdmin(orderId, req.reason())));
    }

    @PatchMapping("/{orderId}/refund")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(summary = "[ADMIN] Issue a refund (devolución) on a paid order, applying the refund policy")
    public ResponseEntity<RefundResponse> refund(
            @PathVariable String orderId,
            @RequestBody RefundRequest req
    ) {
        Order order = getOrderUseCase.getById(orderId);

        // La política decide CASH / STORE_CREDIT / DENY a partir de hechos autoritativos
        // del pedido (tipo de entrega, fecha) + hechos de consumo que aporta el admin/UI.
        RefundContext ctx = new RefundContext(
                order.getDeliveryType(),
                order.getCreatedAt(),
                Instant.now(),
                Boolean.TRUE.equals(req.downloaded()),
                req.readProgress() == null ? 0 : req.readProgress(),
                req.adminOverride());

        var resolution = refundDecisionService.resolve(ctx, order.getTotal().amount());
        if (resolution.isDenied()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, resolution.decision().reason());
        }

        String note = resolution.isStoreCredit()
                ? appendCredit(req.reason(), resolution.storeCreditCode())
                : req.reason();
        Order updated = updateStatusUseCase.refund(orderId, note, req.restock());

        return ResponseEntity.ok(new RefundResponse(
                OrderResponse.from(updated),
                resolution.decision().outcome().name(),
                resolution.storeCreditCode(),
                resolution.decision().reason()));
    }

    private static String appendCredit(String reason, String code) {
        String base = (reason == null || reason.isBlank()) ? "Reembolso" : reason;
        return base + " — crédito en tienda emitido: " + code;
    }

    /**
     * @param downloaded    si el cliente descargó/abrió el libro (digital)
     * @param readProgress  progreso de lectura 0-100 (digital)
     * @param adminOverride fuerza el reembolso en efectivo saltándose ventana y consumo
     */
    public record RefundRequest(String reason, boolean restock, Boolean downloaded,
                                Integer readProgress, boolean adminOverride) {}

    public record RefundResponse(OrderResponse order, String outcome,
                                 String storeCreditCode, String policyReason) {}

    @GetMapping("/admin/shipments")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN','REPARTIDOR')")
    @Operation(summary = "[ADMIN/REPARTIDOR] Fulfillment queue: physical orders to ship/deliver")
    public ResponseEntity<List<OrderResponse>> shipmentsQueue(@AuthenticationPrincipal Jwt jwt) {
        List<OrderResponse> orders = getOrderUseCase.getShipmentsQueue(jwt.getSubject(), isAdmin(jwt))
                .stream().map(OrderResponse::from).toList();
        return ResponseEntity.ok(orders);
    }

    @PatchMapping("/{orderId}/claim-delivery")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN','REPARTIDOR')")
    @Operation(summary = "[REPARTIDOR] Claim an unassigned delivery")
    public ResponseEntity<OrderResponse> claimDelivery(
            @AuthenticationPrincipal Jwt jwt, @PathVariable String orderId) {
        String courierName = jwt.getClaimAsString("username");
        if (courierName == null || courierName.isBlank()) courierName = jwt.getClaimAsString("email");
        return ResponseEntity.ok(OrderResponse.from(
                updateStatusUseCase.claimDelivery(orderId, jwt.getSubject(), courierName)));
    }

    @PatchMapping("/{orderId}/ship")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN','REPARTIDOR')")
    @Operation(summary = "[ADMIN/REPARTIDOR] Mark order as shipped (with carrier + tracking)")
    public ResponseEntity<OrderResponse> ship(
            @PathVariable String orderId,
            @Valid @RequestBody ShipOrderRequest req
    ) {
        return ResponseEntity.ok(OrderResponse.from(
                updateStatusUseCase.ship(orderId, req.carrier(), req.trackingNumber())));
    }

    @PatchMapping("/{orderId}/deliver")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN','REPARTIDOR')")
    @Operation(summary = "[ADMIN/REPARTIDOR] Mark order as delivered (requires the customer's delivery code)")
    public ResponseEntity<OrderResponse> deliver(
            @PathVariable String orderId,
            @Valid @RequestBody DeliverOrderRequest req
    ) {
        return ResponseEntity.ok(OrderResponse.from(
                updateStatusUseCase.deliver(orderId, req.deliveryCode(), req.receivedBy())));
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private boolean isAdmin(Jwt jwt) {
        Object roles = jwt.getClaim("roles");
        if (roles instanceof Iterable<?> list)
            for (Object r : list)
                if ("ROLE_ADMIN".equals(r) || "ROLE_SUPERADMIN".equals(r)) return true;
        return false;
    }
}
