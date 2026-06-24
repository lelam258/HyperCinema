package com.cinema.hyperCinema.exception.seat;

import java.io.Serial;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class SeatValidationException extends SeatException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String key;

    public SeatValidationException(String key) {
        super(key);
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
