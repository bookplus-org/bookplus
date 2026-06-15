package com.bookplus.catalog.domain.port.out;

import com.bookplus.catalog.domain.model.Review;

public interface SaveReviewPort {
    Review save(Review review);
}
