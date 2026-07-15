package com.cinema.hyperCinema.dto.staff.booking;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookingTicketView {

    private final Integer ticketId;
    private final String seatLabel;
    private final String seatType;
    private final String qrCode;
    private final String status;
}
