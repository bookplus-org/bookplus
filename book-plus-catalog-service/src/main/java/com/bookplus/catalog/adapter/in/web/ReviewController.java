package com.bookplus.catalog.adapter.in.web;

import com.bookplus.catalog.adapter.in.web.dto.AddReviewRequest;
import com.bookplus.catalog.adapter.in.web.dto.ReviewResponse;
import com.bookplus.catalog.domain.port.in.AddReviewUseCase;
import com.bookplus.catalog.domain.port.in.AddReviewUseCase.AddReviewCommand;
import com.bookplus.catalog.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * Adapter IN — Controlador de reseñas.
 * Requiere autenticación JWT para POST.
 */
@RestController
@RequestMapping("/api/v1/books/{bookId}/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Book review endpoints")
public class ReviewController {

    private final AddReviewUseCase addReviewUseCase;

    // ── POST /api/v1/books/{bookId}/reviews ─────────────────────────────────

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Add a review to a book",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<ReviewResponse>> addReview(
            @PathVariable                       String bookId,
            @Valid @RequestBody                 AddReviewRequest request,
            @AuthenticationPrincipal            Jwt jwt
    ) {
        String userId   = jwt.getSubject();
        String username = jwt.getClaimAsString("preferred_username");
        if (username == null || username.isBlank()) {
            username = jwt.getClaimAsString("username");
        }

        AddReviewCommand command = new AddReviewCommand(
                bookId,
                userId,
                username,
                request.rating(),
                request.comment(),
                request.verifiedPurchase()
        );

        ReviewResponse response = ReviewResponse.from(addReviewUseCase.add(command));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response));
    }
}
