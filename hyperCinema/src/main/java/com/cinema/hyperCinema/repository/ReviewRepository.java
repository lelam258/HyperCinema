package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {

    @Query("SELECT r FROM Review r JOIN FETCH r.user WHERE r.movie.movieId = :movieId ORDER BY r.createdAt DESC")
    List<Review> findByMovieIdWithUser(@Param("movieId") Integer movieId);

    List<Review> findByUser_UserId(Integer userId);

    Optional<Review> findByUser_UserIdAndMovie_MovieId(Integer userId, Integer movieId);
}
