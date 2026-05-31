package com.cinema.hyperCinema.dto.admin.branch.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BranchStatusChangeRequest {

    @NotBlank(message = "Trạng thái không được để trống")
    @Pattern(
            regexp = "^(Active|Inactive|Maintenance)$",
            message = "{branch.status.invalid}"
    )
    private String status;
}
