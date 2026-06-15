package com.bookplus.inventory.adapter.in.web;

import com.bookplus.inventory.adapter.in.web.dto.*;
import com.bookplus.inventory.domain.port.in.*;
import com.bookplus.inventory.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Adapter IN — REST endpoints del Inventory Service.
 *
 * GET  /api/v1/inventory/{bookId}                — stock actual (público)
 * GET  /api/v1/inventory/{bookId}/movements      — historial (ADMIN)
 * POST /api/v1/inventory                         — inicializar stock (ADMIN)
 * POST /api/v1/inventory/{bookId}/add            — añadir stock (ADMIN)
 * PUT  /api/v1/inventory/{bookId}/adjust         — ajustar stock (ADMIN)
 */
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Stock management endpoints")
public class InventoryController {

    private final GetStockUseCase           getStockUseCase;
    private final InitializeStockUseCase    initializeStockUseCase;
    private final AddStockUseCase           addStockUseCase;
    private final AdjustStockUseCase        adjustStockUseCase;
    private final GetStockMovementsUseCase  getStockMovementsUseCase;

    // ── GET /api/v1/inventory/{bookId} ────────────────────────────────────

    @GetMapping("/{bookId}")
    @Operation(summary = "Get current stock for a book")
    public ResponseEntity<ApiResponse<StockResponse>> getStock(@PathVariable String bookId) {
        return ResponseEntity.ok(ApiResponse.success(
                StockResponse.from(getStockUseCase.getByBookId(bookId))));
    }

    // ── GET /api/v1/inventory/{bookId}/movements ──────────────────────────

    @GetMapping("/{bookId}/movements")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get stock movement history for a book")
    public ResponseEntity<ApiResponse<List<MovementResponse>>> getMovements(
            @PathVariable                      String bookId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        List<MovementResponse> movements = getStockMovementsUseCase
                .getByBookId(bookId, page, size)
                .stream().map(MovementResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.success(movements));
    }

    // ── POST /api/v1/inventory ────────────────────────────────────────────

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Initialize stock for a book")
    public ResponseEntity<ApiResponse<StockResponse>> initialize(
            @Valid @RequestBody InitializeStockRequest request) {

        var command = new InitializeStockUseCase.InitializeStockCommand(
                request.bookId(), request.initialQuantity(), request.lowStockThreshold());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(StockResponse.from(initializeStockUseCase.initialize(command))));
    }

    // ── POST /api/v1/inventory/{bookId}/add ──────────────────────────────

    @PostMapping("/{bookId}/add")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Add units to a book's stock")
    public ResponseEntity<ApiResponse<StockResponse>> addStock(
            @PathVariable String bookId,
            @Valid @RequestBody AddStockRequest request) {

        var command = new AddStockUseCase.AddStockCommand(
                bookId, request.quantity(), request.referenceId(), request.notes());

        return ResponseEntity.ok(ApiResponse.success(
                StockResponse.from(addStockUseCase.addStock(command))));
    }

    // ── PUT /api/v1/inventory/{bookId}/adjust ─────────────────────────────

    @PutMapping("/{bookId}/adjust")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Adjust total stock for a book (physical inventory correction)")
    public ResponseEntity<ApiResponse<StockResponse>> adjust(
            @PathVariable String bookId,
            @Valid @RequestBody AdjustStockRequest request) {

        var command = new AdjustStockUseCase.AdjustStockCommand(
                bookId, request.newTotalQuantity(), request.lowStockThreshold(), request.notes());

        return ResponseEntity.ok(ApiResponse.success(
                StockResponse.from(adjustStockUseCase.adjust(command))));
    }
}
