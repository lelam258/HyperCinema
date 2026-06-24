package com.cinema.hyperCinema.exception.seat;

import java.io.Serial;

public class SeatException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public SeatException(String message) {
        super(message);
    }
}
