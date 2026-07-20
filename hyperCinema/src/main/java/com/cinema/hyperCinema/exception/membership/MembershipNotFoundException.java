package com.cinema.hyperCinema.exception.membership;

public class MembershipNotFoundException extends MembershipException {
    public MembershipNotFoundException(String key) {
        super(key);
    }

    public static MembershipNotFoundException plan() {
        return new MembershipNotFoundException("membership.plan.not_found");
    }

    public static MembershipNotFoundException userMembership() {
        return new MembershipNotFoundException("membership.user_membership.not_found");
    }
}
