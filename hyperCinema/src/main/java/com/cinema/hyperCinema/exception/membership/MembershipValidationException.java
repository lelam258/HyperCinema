package com.cinema.hyperCinema.exception.membership;

public class MembershipValidationException extends MembershipException {
    public MembershipValidationException(String key) {
        super(key);
    }
}
