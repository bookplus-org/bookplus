package com.bookplus.catalog.domain.port.in;

import com.bookplus.catalog.domain.model.Review;

public interface AddReviewUseCase {
    Review add(AddReviewCommand command);

    record AddReviewCommand(
            String bookId, String userId, String username,
            int rating, String comment, boolean verifiedPurchase
    ) {}
}
