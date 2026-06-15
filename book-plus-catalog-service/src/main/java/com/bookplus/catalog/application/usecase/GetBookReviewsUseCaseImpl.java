package com.bookplus.catalog.application.usecase;

import com.bookplus.catalog.domain.model.BookId;
import com.bookplus.catalog.domain.model.Review;
import com.bookplus.catalog.domain.port.in.GetBookReviewsUseCase;
import com.bookplus.catalog.domain.port.in.PagedResult;
import com.bookplus.catalog.domain.port.out.LoadReviewPort;
import com.bookplus.catalog.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class GetBookReviewsUseCaseImpl implements GetBookReviewsUseCase {

    private final LoadReviewPort loadReviewPort;

    @Override
    public PagedResult<Review> getByBook(String bookId, int page, int size) {
        log.debug("Getting reviews: bookId='{}' page={} size={}", bookId, page, size);
        return loadReviewPort.findByBookId(BookId.of(bookId), page, size);
    }
}
