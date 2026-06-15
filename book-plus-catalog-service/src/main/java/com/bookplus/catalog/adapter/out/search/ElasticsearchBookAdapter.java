package com.bookplus.catalog.adapter.out.search;

import com.bookplus.catalog.domain.model.*;
import com.bookplus.catalog.domain.port.in.PagedResult;
import com.bookplus.catalog.domain.port.in.SearchBooksUseCase.SearchQuery;
import com.bookplus.catalog.domain.port.out.IndexBookPort;
import com.bookplus.catalog.domain.port.out.SearchBooksPort;
import com.bookplus.catalog.shared.annotation.PersistenceAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;

import java.util.List;

/**
 * Adapter OUT — Elasticsearch.
 * Implementa SearchBooksPort (full-text) e IndexBookPort (index/remove).
 */
@PersistenceAdapter
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchBookAdapter implements SearchBooksPort, IndexBookPort {

    private final BookSearchRepository    bookSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    // ── SearchBooksPort ───────────────────────────────────────────────────

    @Override
    public PagedResult<Book> search(SearchQuery query) {
        log.debug("ES search: q='{}' page={} size={}", query.query(), query.page(), query.size());

        // Multi-field criteria: title, author, isbn, description
        Criteria criteria = new Criteria("title").matches(query.query())
                .or(new Criteria("author").matches(query.query()))
                .or(new Criteria("isbn").is(query.query()))
                .or(new Criteria("description").matches(query.query()));

        CriteriaQuery cq = new CriteriaQuery(criteria,
                PageRequest.of(query.page(), query.size()));

        SearchHits<BookDocument> hits = elasticsearchOperations.search(cq, BookDocument.class);

        List<Book> books = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(this::toDomain)
                .toList();

        long total = hits.getTotalHits();
        int totalPages = (int) Math.ceil((double) total / query.size());
        int page = query.page();

        return new PagedResult<>(books, page, query.size(), total, totalPages,
                page == 0, page >= totalPages - 1);
    }

    // ── IndexBookPort ─────────────────────────────────────────────────────

    @Override
    public void index(Book book) {
        log.debug("Indexing book in ES: id={}", book.getId().value());
        bookSearchRepository.save(BookDocument.from(book));
    }

    @Override
    public void remove(BookId bookId) {
        log.debug("Removing book from ES index: id={}", bookId.value());
        bookSearchRepository.deleteById(bookId.value().toString());
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private Book toDomain(BookDocument doc) {
        return Book.reconstitute(
                BookId.of(doc.id()),
                ISBN.of(doc.isbn()),
                doc.title(),
                Slug.of(doc.slug()),
                doc.author(),
                doc.description(),
                Money.of(doc.price(), doc.currency()),
                doc.discountPrice() != null ? Money.of(doc.discountPrice(), doc.currency()) : null,
                doc.imageUrl(),
                null,                 // previewUrl not stored in the search index
                doc.publisher(),
                doc.publishedDate(),
                doc.language(),
                doc.pages(),
                CategoryId.of(doc.categoryId()),
                doc.active(),
                doc.stockSnapshot(),
                doc.averageRating(),
                doc.reviewCount(),
                doc.createdAt(),
                doc.createdAt()   // updatedAt not stored — use createdAt as fallback
        );
    }
}
