package com.cinema.hyperCinema.dto.admin.movie.response;

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
public class GenreSummary {

    private Integer genreId;

    private String name;
}
