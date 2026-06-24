package com.cinema.hyperCinema.dto.admin.seat.response;

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
public class SeatManagementContext {

    private boolean admin;

    private String sidebar;

    private Integer hallId;

    private String hallName;

    private String branchName;
}
