package com.cinema.hyperCinema.dto.admin.showtime.response;

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
public class MovieOption {

    private Integer movieId;

    private String title;

    private Integer duration;

    private String status;
}
