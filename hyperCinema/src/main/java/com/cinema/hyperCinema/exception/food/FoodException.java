package com.cinema.hyperCinema.exception.food;

import java.io.Serial;

public class FoodException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public FoodException(String message) {
        super(message);
    }
}
