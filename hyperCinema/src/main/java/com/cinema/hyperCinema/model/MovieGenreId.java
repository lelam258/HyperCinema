package com.cinema.hyperCinema.model;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

@Embeddable
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode
public class MovieGenreId implements Serializable {

    @Column(name = "movie_id")
    private Integer movieId;

    @Column(name = "genre_id")
    private Integer genreId;
}
