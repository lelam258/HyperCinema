package com.cinema.hyperCinema.exception.hall;

import java.io.Serial;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class HallNotFoundException extends HallException {

    @Serial
    private static final long serialVersionUID = 1L;

    public HallNotFoundException(Integer hallId) {
        super("hall.not_found");
    }
}
