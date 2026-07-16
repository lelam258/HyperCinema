package com.cinema.hyperCinema.dto.ui.workspace;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CustomerMembershipProgressView {

    private final boolean active;
    private final boolean highestTier;
    private final String currentTier;
    private final BigDecimal discountPercent;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final long pointsBalance;
    private final String nextTier;
    private final long nextTierThreshold;
    private final long pointsNeeded;
    private final int progressPercent;
    private final String statusText;
}
