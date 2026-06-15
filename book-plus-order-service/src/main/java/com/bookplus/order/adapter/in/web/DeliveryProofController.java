package com.bookplus.order.adapter.in.web;

import com.bookplus.order.adapter.in.web.dto.OrderResponse;
import com.bookplus.order.adapter.out.persistence.entity.DeliveryProofEntity;
import com.bookplus.order.adapter.out.persistence.repository.DeliveryProofJpaRepository;
import com.bookplus.order.domain.model.Order;
import com.bookplus.order.domain.port.in.GetOrderUseCase;
import com.bookplus.order.domain.port.in.UpdateOrderStatusUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

/**
 * Prueba de entrega física: foto del paquete + firma del receptor.
 *  - POST /api/v1/orders/{id}/deliver-proof  (ADMIN/REPARTIDOR) marca entregado y guarda la prueba.
 *  - GET  /api/v1/orders/{id}/proof/photo     (dueño o ADMIN/REPARTIDOR)
 *  - GET  /api/v1/orders/{id}/proof/signature (dueño o ADMIN/REPARTIDOR)
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Delivery Proof", description = "Photo + signature proof of delivery")
@SecurityRequirement(name = "bearerAuth")
public class DeliveryProofController {

    private static final long MAX_BYTES = 5L * 1024 * 1024; // 5 MB

    private final UpdateOrderStatusUseCase  updateStatusUseCase;
    private final GetOrderUseCase           getOrderUseCase;
    private final DeliveryProofJpaRepository proofRepo;

    @PostMapping(path = "/{orderId}/deliver-proof", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN','REPARTIDOR')")
    @Operation(summary = "[ADMIN/REPARTIDOR] Mark delivered with photo + signature proof")
    public ResponseEntity<OrderResponse> deliverWithProof(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String orderId,
            @RequestParam("deliveryCode") String deliveryCode,
            @RequestParam(value = "receivedBy", required = false) String receivedBy,
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            @RequestParam(value = "signature", required = false) MultipartFile signature
    ) {
        if (photo == null || photo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La foto de entrega es obligatoria");
        }
        validateImage(photo);
        if (signature != null && !signature.isEmpty()) validateImage(signature);

        // Valida el código y marca el pedido como entregado (lanza 400 si el código es incorrecto)
        Order order = updateStatusUseCase.deliver(orderId, deliveryCode, receivedBy);

        String deliveredBy = jwt.getClaimAsString("username");
        if (deliveredBy == null || deliveredBy.isBlank()) deliveredBy = jwt.getClaimAsString("email");

        try {
            proofRepo.save(DeliveryProofEntity.builder()
                    .orderId(order.getId().value())
                    .photo(photo.getBytes())
                    .photoContentType(photo.getContentType())
                    .signature(signature != null && !signature.isEmpty() ? signature.getBytes() : null)
                    .signatureContentType(signature != null && !signature.isEmpty() ? signature.getContentType() : null)
                    .receivedBy(receivedBy)
                    .deliveredBy(deliveredBy)
                    .createdAt(Instant.now())
                    .build());
        } catch (java.io.IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo leer la imagen");
        }

        return ResponseEntity.ok(OrderResponse.from(order));
    }

    @GetMapping("/{orderId}/proof/photo")
    @Operation(summary = "Proof photo (owner or admin/courier). 204 if none.")
    public ResponseEntity<byte[]> photo(@AuthenticationPrincipal Jwt jwt, @PathVariable String orderId) {
        authorizeView(jwt, orderId);
        return proofRepo.findById(UUID.fromString(orderId))
                .filter(p -> p.getPhoto() != null)
                .map(p -> ResponseEntity.ok()
                        .contentType(mediaType(p.getPhotoContentType()))
                        .cacheControl(CacheControl.noCache())
                        .body(p.getPhoto()))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{orderId}/proof/signature")
    @Operation(summary = "Proof signature (owner or admin/courier). 204 if none.")
    public ResponseEntity<byte[]> signature(@AuthenticationPrincipal Jwt jwt, @PathVariable String orderId) {
        authorizeView(jwt, orderId);
        return proofRepo.findById(UUID.fromString(orderId))
                .filter(p -> p.getSignature() != null)
                .map(p -> ResponseEntity.ok()
                        .contentType(mediaType(p.getSignatureContentType()))
                        .cacheControl(CacheControl.noCache())
                        .body(p.getSignature()))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void validateImage(MultipartFile file) {
        String ct = file.getContentType();
        if (ct == null || !ct.toLowerCase().startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Debe ser una imagen");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Máximo 5 MB por imagen");
        }
    }

    private void authorizeView(Jwt jwt, String orderId) {
        if (isAdminOrCourier(jwt)) return;
        Order order = getOrderUseCase.getById(orderId);
        if (!order.getUserId().equals(jwt.getSubject())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes ver la prueba de un pedido ajeno");
        }
    }

    private boolean isAdminOrCourier(Jwt jwt) {
        Object roles = jwt.getClaim("roles");
        if (roles instanceof Iterable<?> list)
            for (Object r : list)
                if ("ROLE_ADMIN".equals(r) || "ROLE_SUPERADMIN".equals(r) || "ROLE_REPARTIDOR".equals(r))
                    return true;
        return false;
    }

    private static MediaType mediaType(String ct) {
        try {
            return ct != null ? MediaType.parseMediaType(ct) : MediaType.IMAGE_JPEG;
        } catch (Exception e) {
            return MediaType.IMAGE_JPEG;
        }
    }
}
