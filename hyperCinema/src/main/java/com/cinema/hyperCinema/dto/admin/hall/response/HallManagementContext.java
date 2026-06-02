package com.cinema.hyperCinema.dto.admin.hall.response;

import java.util.List;

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
public class HallManagementContext {

    private boolean admin;

    private String sidebar;

    private BranchOption lockedBranch;

    private List<BranchOption> branchOptions;
}
