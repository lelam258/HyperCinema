package com.cinema.hyperCinema.exception.hall;

import java.io.Serial;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class HallValidationException extends HallException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String key;

    public HallValidationException(String key) {
        super(key);
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
