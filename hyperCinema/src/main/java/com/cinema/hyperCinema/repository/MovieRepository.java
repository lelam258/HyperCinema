package com.cinema.hyperCinema.repository;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cinema.hyperCinema.model.Movie;

@Repository
public interface MovieRepository
        extends JpaRepository<Movie, Integer>, JpaSpecificationExecutor<Movie> {

    boolean existsByTitleIgnoreCaseAndReleaseDate(String title, LocalDate releaseDate);

    boolean existsByTitleIgnoreCaseAndReleaseDateAndMovieIdNot(
            String title, LocalDate releaseDate, Integer movieId);

    long countByStatus(String status);
}
