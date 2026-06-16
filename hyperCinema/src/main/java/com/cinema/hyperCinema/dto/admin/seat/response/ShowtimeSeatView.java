package com.cinema.hyperCinema.dto.admin.seat.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowtimeSeatView {
    private Integer seatId;
    private String seatRow;
    private Integer seatNumber;
    private String type; // Standard, VIP, Double
    private String status; // AVAILABLE, BOOKED, RESERVED
    private Integer finalPrice;
}
