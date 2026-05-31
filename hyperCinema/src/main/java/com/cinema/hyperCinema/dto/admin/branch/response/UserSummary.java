package com.cinema.hyperCinema.dto.admin.branch.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSummary {

    private Integer userId;

    private String fullName;

    private String email;

    private String phone;

    private String role;

    private String status;
}
