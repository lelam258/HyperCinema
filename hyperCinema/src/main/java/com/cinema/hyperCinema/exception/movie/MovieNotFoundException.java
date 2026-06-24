package com.cinema.hyperCinema.exception.movie;

import java.io.Serial;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class MovieNotFoundException extends MovieException {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String KEY = "movie.not_found";

    public MovieNotFoundException() {
        super(KEY, HttpStatus.NOT_FOUND);
    }

    public MovieNotFoundException(Integer movieId) {
        super(KEY, HttpStatus.NOT_FOUND, movieId);
    }

    public MovieNotFoundException(Integer movieId, Throwable cause) {
        super(KEY, HttpStatus.NOT_FOUND, cause);
    }
}
