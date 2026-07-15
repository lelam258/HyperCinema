package com.cinema.hyperCinema.dto.report.revenue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueReportView {

    private String scopeRole;

    private Integer branchId;

    private String branchName;

    private LocalDate dateFrom;

    private LocalDate dateTo;

    private String preset;

    private long totalRevenue;

    private long ticketRevenue;

    private long foodRevenue;

    private long paidBookingCount;

    private long ticketCount;

    private long averageOrderValue;

    private List<RevenueTrendPoint> trend;

    private List<BranchRevenueRankingRow> branchRanking;

    private BranchCoverageSummary coverageSummary;

    private List<BranchCoverageSummary> branchCoverage;

    private List<ShowtimeCoverageRow> showtimeCoverage;

    private String lastUpdated;

    private boolean noScope;

    private String message;
}
