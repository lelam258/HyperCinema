package com.cinema.hyperCinema.dto.admin.seat.response;

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
public class SeatListItem {

    private Integer seatId;

    private String seatRow;

    private Integer seatNumber;

    private String type;

    private String maintenanceStatus;

    private boolean hasActiveReference;
}
