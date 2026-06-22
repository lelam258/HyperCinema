package com.cinema.hyperCinema.dto.admin.branch.response;

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
public class HallSummary {

    private Integer hallId;

    private String name;

    private Integer capacity;

    private String hallType;

    private String status;
}
