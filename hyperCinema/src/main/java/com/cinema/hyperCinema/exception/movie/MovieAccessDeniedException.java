package com.cinema.hyperCinema.exception.movie;

import java.io.Serial;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class MovieAccessDeniedException extends MovieException {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String KEY = "movie.access_denied";

    public MovieAccessDeniedException() {
        super(KEY, HttpStatus.FORBIDDEN);
    }

    public MovieAccessDeniedException(String key) {
        super(key, HttpStatus.FORBIDDEN);
    }

    public MovieAccessDeniedException(String key, Throwable cause) {
        super(key, HttpStatus.FORBIDDEN, cause);
    }
}
