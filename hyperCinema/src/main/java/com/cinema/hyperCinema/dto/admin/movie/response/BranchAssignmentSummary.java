package com.cinema.hyperCinema.dto.admin.movie.response;

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
public class BranchAssignmentSummary {

    private Integer branchId;

    private String branchName;

    private String city;

    private LocalDateTime assignedAt;
}
