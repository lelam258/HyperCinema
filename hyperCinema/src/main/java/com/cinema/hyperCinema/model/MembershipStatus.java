package com.cinema.hyperCinema.model;

import java.util.Locale;

public enum MembershipStatus {
    ACTIVE,
    INACTIVE,
    CANCELLED;

    public static boolean isAllowed(String value) {
        return parse(value) != null;
    }

    public static MembershipStatus parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return MembershipStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static String normalize(String value) {
        MembershipStatus status = parse(value);
        return status == null ? null : status.name();
    }
}
