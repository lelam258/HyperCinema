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
public class MovieDetailView {

    private Integer movieId;

    private String title;

    private Integer duration;

    private String description;

    private LocalDate releaseDate;

    private String status;

    private String posterUrl;

    private String trailerUrl;

    private Integer languageId;

    private String languageName;

    private List<GenreSummary> genres;

    private List<BranchAssignmentSummary> branches;

    private long futureShowtimeCount;

    private long pastShowtimeCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
