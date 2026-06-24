package com.cinema.hyperCinema.exception.movie;

import java.io.Serial;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class MovieValidationException extends MovieException {

    @Serial
    private static final long serialVersionUID = 1L;

    public MovieValidationException(String key) {
        super(key, HttpStatus.BAD_REQUEST);
    }

    public MovieValidationException(String key, HttpStatus status) {
        super(key, status);
    }

    public MovieValidationException(String key, Object... args) {
        super(key, HttpStatus.BAD_REQUEST, args);
    }

    public MovieValidationException(String key, Throwable cause) {
        super(key, HttpStatus.BAD_REQUEST, cause);
    }
}
