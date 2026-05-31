package com.cinema.hyperCinema.dto.admin.branch.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssignStaffRequest {

    @NotNull(message = "userId không được để trống")
    private Integer userId;

    @NotNull(message = "managerId không được để trống")
    private Integer managerId;
}
