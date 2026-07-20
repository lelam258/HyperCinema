package com.cinema.hyperCinema.service.payment;

public record PaymentCallbackResult(Status status, String message) {

    public enum Status {
        CONFIRMED,
        ALREADY_CONFIRMED,
        ORDER_NOT_FOUND,
        PAYMENT_NOT_FOUND,
        INVALID_AMOUNT,
        EXPIRED,
        INVALID_STATE,
        FAILED
    }

    public boolean accepted() {
        return status == Status.CONFIRMED || status == Status.ALREADY_CONFIRMED;
    }
}
