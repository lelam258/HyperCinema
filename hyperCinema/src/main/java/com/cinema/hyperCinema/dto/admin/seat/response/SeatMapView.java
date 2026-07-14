package com.cinema.hyperCinema.dto.admin.seat.response;

import java.util.List;

import com.cinema.hyperCinema.dto.admin.hall.response.SeatTypePriceView;
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
public class SeatMapView {

    private Integer hallId;

    private String hallName;

    private String branchName;

    private List<SeatTypePriceView> seatTypePrices;

    private List<SeatListItem> seats;

    private long totalSeats;

    private boolean empty;
}
