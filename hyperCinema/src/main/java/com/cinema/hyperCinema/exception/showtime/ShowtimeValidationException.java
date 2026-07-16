package com.cinema.hyperCinema.exception.showtime;

public class ShowtimeValidationException extends ShowtimeException {

    private final String key;

    public ShowtimeValidationException(String key) {
        super(key);
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
