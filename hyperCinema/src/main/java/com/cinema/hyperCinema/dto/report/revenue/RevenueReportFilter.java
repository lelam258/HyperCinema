package com.cinema.hyperCinema.dto.report.revenue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueReportFilter {

    private LocalDate dateFrom;

    private LocalDate dateTo;

    private String preset;

    private Integer branchId;
}
