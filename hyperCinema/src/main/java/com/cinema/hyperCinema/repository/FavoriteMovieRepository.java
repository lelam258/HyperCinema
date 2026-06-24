package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.FavoriteMovie;
import com.cinema.hyperCinema.model.FavoriteMovieId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FavoriteMovieRepository extends JpaRepository<FavoriteMovie, FavoriteMovieId> {

    boolean existsByIdMovieId(Integer movieId);
}
