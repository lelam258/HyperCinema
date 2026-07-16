package com.cinema.hyperCinema.repository;

import java.time.LocalDate;
import java.util.List;

import com.cinema.hyperCinema.model.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public interface MovieRepository
        extends JpaRepository<Movie, Integer>, JpaSpecificationExecutor<Movie> {

    boolean existsByTitleIgnoreCaseAndReleaseDate(String title, LocalDate releaseDate);

    boolean existsByTitleIgnoreCaseAndReleaseDateAndMovieIdNot(
            String title, LocalDate releaseDate, Integer movieId);

    long countByStatus(String status);

    List<Movie> findByStatusNotIgnoreCase(String status, Sort sort);
}
