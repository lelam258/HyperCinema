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
public class RevenueTrendPoint {

    private LocalDate date;

    private String label;

    private long revenue;

    private long paidBookingCount;
}
