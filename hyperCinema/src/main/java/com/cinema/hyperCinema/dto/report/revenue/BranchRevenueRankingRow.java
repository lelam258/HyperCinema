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
public class BranchRevenueRankingRow {

    private int rank;

    private Integer branchId;

    private String branchName;

    private long totalRevenue;

    private long ticketRevenue;

    private long foodRevenue;

    private long paidBookingCount;

    private long ticketCount;

    private double contributionPercent;
}
