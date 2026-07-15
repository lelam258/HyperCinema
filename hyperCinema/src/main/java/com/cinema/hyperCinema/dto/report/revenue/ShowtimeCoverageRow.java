package com.cinema.hyperCinema.dto.report.revenue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowtimeCoverageRow {

    private Integer showtimeId;

    private String movieTitle;

    private Integer branchId;

    private String branchName;

    private String hallName;

    private LocalDateTime startTime;

    private long capacity;

    private long paidTicketCount;

    private double seatCoveragePercent;

    private long actualTicketRevenue;

    private long potentialTicketRevenue;

    private double revenueCoveragePercent;

    private long revenueGap;

    private ShowtimeCoverageEvaluation evaluation;

    private String evaluationLabel;
}
