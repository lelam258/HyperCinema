package com.cinema.hyperCinema.dto.review;

import java.util.List;

public record ReviewSummaryResponse(
        String movieTitle,
        int reviewCount,
        int positivePercent,
        int neutralPercent,
        int negativePercent,
        String summary,
        List<ReviewAspectSummary> aspects,
        boolean aiGenerated
) {
    public record ReviewAspectSummary(
            String name,
            int reviewCount,
            int positivePercent,
            int neutralPercent,
            int negativePercent,
            String summary
    ) {
    }
}
