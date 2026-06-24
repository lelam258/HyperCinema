package com.cinema.hyperCinema.exception.food;

import java.io.Serial;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class FoodValidationException extends FoodException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String key;

    public FoodValidationException(String key) {
        super(key);
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
