package com.cinema.hyperCinema.dto.staff.booking;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookingFoodOrderView {

    private final Integer orderId;
    private final String status;
    private final Integer totalAmount;
    private final LocalDateTime createdAt;
}
