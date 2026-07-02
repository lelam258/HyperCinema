package com.cinema.hyperCinema.dto.staff.booking;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookingManagementSummary {

    private final long total;
    private final long pending;
    private final long paid;
    private final long cancelled;
}
