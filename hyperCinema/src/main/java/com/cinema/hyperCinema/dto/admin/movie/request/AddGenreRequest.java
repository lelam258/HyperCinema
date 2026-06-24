package com.cinema.hyperCinema.dto.admin.movie.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddGenreRequest {

    @NotNull
    @Positive
    private Integer genreId;
}
