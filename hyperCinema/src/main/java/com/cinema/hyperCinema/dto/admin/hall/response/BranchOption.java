package com.cinema.hyperCinema.dto.admin.hall.response;

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
public class BranchOption {

    private Integer branchId;

    private String name;

    private String city;
}
