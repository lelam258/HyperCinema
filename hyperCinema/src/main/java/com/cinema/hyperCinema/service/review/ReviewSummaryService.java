package com.cinema.hyperCinema.service.review;

import com.cinema.hyperCinema.dto.review.ReviewSummaryResponse;

public interface ReviewSummaryService {
    ReviewSummaryResponse summarize(Integer movieId);
}
