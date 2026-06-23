package com.cinema.hyperCinema.exception.seat;

import java.io.Serial;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class SeatNotFoundException extends SeatException {

    @Serial
    private static final long serialVersionUID = 1L;

    public SeatNotFoundException(Integer seatId) {
        super("seat.not_found");
    }
}
