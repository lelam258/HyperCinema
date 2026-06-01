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
public class HallListItem {

    private Integer hallId;

    private String name;

    private Integer branchId;

    private String branchName;

    private String city;

    private String hallType;

    private Integer capacity;

    private String status;

    private Long seatCount;

    private Long showtimeCount;

    private boolean canDelete;
}
