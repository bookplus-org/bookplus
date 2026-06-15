package com.bookplus.catalog.domain.port.in;

import com.bookplus.catalog.domain.model.Review;

public interface GetBookReviewsUseCase {
    PagedResult<Review> getByBook(String bookId, int page, int size);
}
