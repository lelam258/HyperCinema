package com.cinema.hyperCinema.dto.admin.movie.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
public class MovieListItem {

    private Integer movieId;

    private String title;

    private Integer duration;

    private String languageName;

    private List<String> genreNames;

    private LocalDate releaseDate;

    private String status;

    private String posterUrl;

    private LocalDateTime createdAt;
}
