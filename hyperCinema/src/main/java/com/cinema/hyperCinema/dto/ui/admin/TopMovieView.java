package com.cinema.hyperCinema.dto.ui.admin;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopMovieView {

    private Integer movieId;

    private String title;

    private String genre;

    private long bookingCount;
}
