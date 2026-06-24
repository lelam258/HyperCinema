package com.cinema.hyperCinema.dto.admin.showtime.response;

import java.util.List;

import com.cinema.hyperCinema.dto.admin.hall.response.BranchOption;
import com.cinema.hyperCinema.dto.admin.hall.response.HallListItem;

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
public class ShowtimeManagementContext {

    private boolean admin;

    private String sidebar;

    private BranchOption lockedBranch;

    private List<BranchOption> branchOptions;

    private List<HallListItem> hallOptions;

    private List<MovieOption> movieOptions;
}
