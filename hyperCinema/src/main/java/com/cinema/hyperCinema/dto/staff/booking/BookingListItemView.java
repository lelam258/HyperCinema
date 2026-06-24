package com.cinema.hyperCinema.dto.staff.booking;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookingListItemView {

    private final Integer bookingId;
    private final String customerName;
    private final String customerEmail;
    private final String movieTitle;
    private final String branchName;
    private final String hallName;
    private final LocalDateTime showtimeStart;
    private final LocalDateTime createdAt;
    private final Long totalPrice;
    private final String bookingStatus;
    private final String paymentStatus;
    private final List<String> seats;
    private final int ticketCount;
}
