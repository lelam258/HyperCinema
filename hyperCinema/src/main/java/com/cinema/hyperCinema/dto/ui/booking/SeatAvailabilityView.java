package com.cinema.hyperCinema.dto.ui.booking;

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
public class SeatAvailabilityView {

    private Integer seatId;

    private String row;

    private Integer number;

    private String label;

    private String type;

    private Integer price;

    private String displayPrice;

    private String state;

    private boolean selectable;
}
