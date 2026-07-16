package com.cinema.hyperCinema.exception.booking;

public class BookingManagementException extends RuntimeException {

    private final String messageKey;

    public BookingManagementException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
