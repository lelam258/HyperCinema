package com.cinema.hyperCinema.dto.admin.branch.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
