package com.cinema.hyperCinema.dto.auth;

public final class PasswordPolicy {

    public static final int MIN_LENGTH = 6;
    public static final String MIN_LENGTH_MESSAGE = "Mat khau phai tu 6 ky tu tro len";

    private PasswordPolicy() {
    }

    public static boolean isValid(String password) {
        return password != null && password.length() >= MIN_LENGTH;
    }
}
