package com.cinema.hyperCinema.dto.staff.booking;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingManagementFilter {

    private String keyword;

    private String bookingStatus;

    private String paymentStatus;

    private Integer movieId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate showtimeFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate showtimeTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdTo;
}
