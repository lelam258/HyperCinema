package com.cinema.hyperCinema.dto.admin.membership.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserOption {
    private Integer userId;
    private String displayName;
}
