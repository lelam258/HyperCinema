package com.cinema.hyperCinema.dto.report.revenue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchCoverageSummary {

    private Integer branchId;

    private String branchName;

    private long showtimeCount;

    private long totalCapacity;

    private long paidTicketCount;

    private double averageSeatCoveragePercent;

    private long actualTicketRevenue;

    private long potentialTicketRevenue;

    private double averageRevenueCoveragePercent;

    private long revenueGap;

    private long lowCoverageShowtimeCount;
}
