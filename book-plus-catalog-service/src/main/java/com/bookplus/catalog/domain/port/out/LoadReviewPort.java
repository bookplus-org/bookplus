package com.bookplus.catalog.domain.port.out;

import com.bookplus.catalog.domain.model.BookId;
import com.bookplus.catalog.domain.model.Review;
import com.bookplus.catalog.domain.port.in.PagedResult;

public interface LoadReviewPort {
    PagedResult<Review> findByBookId(BookId bookId, int page, int size);
    boolean existsByBookIdAndUserId(BookId bookId, String userId);
}
