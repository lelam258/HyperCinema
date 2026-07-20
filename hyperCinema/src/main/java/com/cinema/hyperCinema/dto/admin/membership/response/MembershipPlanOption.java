package com.cinema.hyperCinema.dto.admin.membership.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MembershipPlanOption {
    private Integer planId;
    private String name;
}
