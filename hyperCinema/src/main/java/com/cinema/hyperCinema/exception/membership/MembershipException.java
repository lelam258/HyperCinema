package com.cinema.hyperCinema.exception.membership;

public class MembershipException extends RuntimeException {

    private final String key;

    public MembershipException(String key) {
        super(key);
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
