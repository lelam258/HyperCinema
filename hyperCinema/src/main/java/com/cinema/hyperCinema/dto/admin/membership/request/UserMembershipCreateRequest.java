package com.cinema.hyperCinema.dto.admin.membership.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserMembershipCreateRequest {

    @NotNull(message = "{membership.user.required}")
    private Integer userId;

    @NotNull(message = "{membership.plan.required}")
    private Integer planId;

    private String status = "ACTIVE";
}
