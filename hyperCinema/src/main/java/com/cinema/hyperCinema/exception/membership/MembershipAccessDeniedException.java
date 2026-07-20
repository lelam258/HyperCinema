package com.cinema.hyperCinema.exception.membership;

public class MembershipAccessDeniedException extends MembershipException {
    public static final String KEY = "membership.access_denied";

    public MembershipAccessDeniedException() {
        super(KEY);
    }
}
