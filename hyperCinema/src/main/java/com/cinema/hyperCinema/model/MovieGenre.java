package com.cinema.hyperCinema.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Movie_Genre")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class MovieGenre {

    @EmbeddedId
    private MovieGenreId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("movieId")
    @JoinColumn(name = "movie_id")
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("genreId")
    @JoinColumn(name = "genre_id")
    private Genre genre;
}
