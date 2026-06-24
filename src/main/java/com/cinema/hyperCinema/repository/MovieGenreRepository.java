package com.cinema.hyperCinema.repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.cinema.hyperCinema.model.Genre;
import com.cinema.hyperCinema.model.MovieGenre;
import com.cinema.hyperCinema.model.MovieGenreId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MovieGenreRepository extends JpaRepository<MovieGenre, MovieGenreId> {

    @Query("SELECT mg.id.genreId FROM MovieGenre mg WHERE mg.id.movieId = :movieId")
    Set<Integer> findGenreIdsByMovieId(@Param("movieId") Integer movieId);

    boolean existsByIdMovieIdAndIdGenreId(Integer movieId, Integer genreId);

    long countByIdMovieId(Integer movieId);

    @Modifying
    @Query("DELETE FROM MovieGenre mg WHERE mg.id.movieId = :movieId AND mg.id.genreId = :genreId")
    void deleteByIdMovieIdAndIdGenreId(@Param("movieId") Integer movieId,
                                       @Param("genreId") Integer genreId);

    @Modifying
    @Query("DELETE FROM MovieGenre mg WHERE mg.id.movieId = :movieId AND mg.id.genreId IN :genreIds")
    void deleteByMovieIdAndGenreIdIn(@Param("movieId") Integer movieId,
                                     @Param("genreIds") Collection<Integer> genreIds);

    @Modifying
    @Query("DELETE FROM MovieGenre mg WHERE mg.id.movieId = :movieId")
    void deleteByMovieId(@Param("movieId") Integer movieId);

    @Query("SELECT g FROM Genre g, MovieGenre mg "
            + "WHERE mg.id.movieId = :movieId AND mg.id.genreId = g.genreId "
            + "ORDER BY g.name ASC")
    List<Genre> findGenresByMovieId(@Param("movieId") Integer movieId);
}
