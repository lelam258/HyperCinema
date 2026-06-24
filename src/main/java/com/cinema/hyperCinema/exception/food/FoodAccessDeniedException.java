package com.cinema.hyperCinema.exception.food;

import java.io.Serial;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class FoodAccessDeniedException extends FoodException {

    @Serial
    private static final long serialVersionUID = 1L;

    public FoodAccessDeniedException() {
        super("food.access_denied");
    }
}
