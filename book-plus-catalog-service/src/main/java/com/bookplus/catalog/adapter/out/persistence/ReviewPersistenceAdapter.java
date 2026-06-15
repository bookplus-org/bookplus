package com.bookplus.catalog.adapter.out.persistence;

import com.bookplus.catalog.adapter.out.persistence.mapper.ReviewPersistenceMapper;
import com.bookplus.catalog.adapter.out.persistence.repository.ReviewJpaRepository;
import com.bookplus.catalog.domain.model.BookId;
import com.bookplus.catalog.domain.model.Review;
import com.bookplus.catalog.domain.port.in.PagedResult;
import com.bookplus.catalog.domain.port.out.LoadReviewPort;
import com.bookplus.catalog.domain.port.out.SaveReviewPort;
import com.bookplus.catalog.shared.annotation.PersistenceAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@PersistenceAdapter
@RequiredArgsConstructor
public class ReviewPersistenceAdapter implements LoadReviewPort, SaveReviewPort {

    private final ReviewJpaRepository    repository;
    private final ReviewPersistenceMapper mapper;

    // ── LoadReviewPort ────────────────────────────────────────────────────

    @Override
    public PagedResult<Review> findByBookId(BookId bookId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Review> result = repository
                .findAllByBookId(bookId.value(), pageable)
                .map(mapper::toDomain);
        return new PagedResult<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isFirst(),
                result.isLast()
        );
    }

    @Override
    public boolean existsByBookIdAndUserId(BookId bookId, String userId) {
        return repository.existsByBookIdAndUserId(bookId.value(), userId);
    }

    // ── SaveReviewPort ────────────────────────────────────────────────────

    @Override
    public Review save(Review review) {
        return mapper.toDomain(repository.save(mapper.toEntity(review)));
    }
}
