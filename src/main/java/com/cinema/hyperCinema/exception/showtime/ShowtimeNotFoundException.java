package com.cinema.hyperCinema.exception.showtime;

public class ShowtimeNotFoundException extends ShowtimeException {

    public ShowtimeNotFoundException(Integer showtimeId) {
        super("Showtime not found: " + showtimeId);
    }
}
