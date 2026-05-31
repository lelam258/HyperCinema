package com.cinema.hyperCinema.dto.admin.branch.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchListItem {

    private Integer branchId;

    private String name;

    private String city;

    private String address;

    private String phone;

    private String status;

    private Long hallCount;

    private LocalDateTime createdAt;
}
