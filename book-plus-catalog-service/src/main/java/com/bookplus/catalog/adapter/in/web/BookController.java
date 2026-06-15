package com.bookplus.catalog.adapter.in.web;

import com.bookplus.catalog.adapter.in.web.dto.*;
import com.bookplus.catalog.domain.model.Book;
import com.bookplus.catalog.domain.port.in.*;
import com.bookplus.catalog.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Adapter IN — Controlador público del catálogo de libros.
 * Expone endpoints de consulta (sin autenticación).
 */
@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
@Tag(name = "Books", description = "Public book catalog endpoints")
public class BookController {

    private final GetBookUseCase       getBookUseCase;
    private final ListBooksUseCase     listBooksUseCase;
    private final SearchBooksUseCase   searchBooksUseCase;
    private final GetBookReviewsUseCase getBookReviewsUseCase;

    // ── GET /api/v1/books/{id} ──────────────────────────────────────────────

    @GetMapping("/{id}")
    @Operation(summary = "Get book by ID")
    public ResponseEntity<ApiResponse<BookResponse>> getById(@PathVariable String id) {
        Book book = getBookUseCase.getById(id);
        return ResponseEntity.ok(ApiResponse.ok(BookResponse.from(book)));
    }

    // ── GET /api/v1/books/isbn/{isbn} ───────────────────────────────────────

    @GetMapping("/isbn/{isbn}")
    @Operation(summary = "Get book by ISBN")
    public ResponseEntity<ApiResponse<BookResponse>> getByIsbn(@PathVariable String isbn) {
        Book book = getBookUseCase.getByIsbn(isbn);
        return ResponseEntity.ok(ApiResponse.ok(BookResponse.from(book)));
    }

    // ── GET /api/v1/books ───────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "List books with optional filters")
    public ResponseEntity<ApiResponse<PagedResponse<BookSummaryResponse>>> list(
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "20")  int size,
            @RequestParam(required = false)      String categoryId,
            @RequestParam(required = false)      String author
    ) {
        ListBooksUseCase.ListBooksQuery query = new ListBooksUseCase.ListBooksQuery(
                page, size, categoryId, author);
        PagedResponse<BookSummaryResponse> response =
                PagedResponse.from(listBooksUseCase.list(query), BookSummaryResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // ── GET /api/v1/books/search ────────────────────────────────────────────

    @GetMapping("/search")
    @Operation(summary = "Full-text search via Elasticsearch")
    public ResponseEntity<ApiResponse<PagedResponse<BookSummaryResponse>>> search(
            @RequestParam                       String q,
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "20")  int size
    ) {
        SearchBooksUseCase.SearchQuery query =
                new SearchBooksUseCase.SearchQuery(q, page, size);
        PagedResponse<BookSummaryResponse> response =
                PagedResponse.from(searchBooksUseCase.search(query), BookSummaryResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // ── GET /api/v1/books/{id}/reviews ─────────────────────────────────────

    @GetMapping("/{id}/reviews")
    @Operation(summary = "Get paginated reviews for a book")
    public ResponseEntity<ApiResponse<PagedResponse<ReviewResponse>>> getReviews(
            @PathVariable                       String id,
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "10")  int size
    ) {
        PagedResponse<ReviewResponse> response =
                PagedResponse.from(getBookReviewsUseCase.getByBook(id, page, size),
                        ReviewResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
