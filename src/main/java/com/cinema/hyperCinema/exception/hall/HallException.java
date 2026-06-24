package com.cinema.hyperCinema.exception.hall;

import java.io.Serial;

public class HallException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public HallException(String message) {
        super(message);
    }
}
