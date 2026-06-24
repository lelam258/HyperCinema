package com.cinema.hyperCinema.dto.admin.seat.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatDetailView {
    private Integer seatId;
    private Integer hallId;
    private String seatRow;
    private Integer seatNumber;
    private String type; // Standard, VIP, Double
}
