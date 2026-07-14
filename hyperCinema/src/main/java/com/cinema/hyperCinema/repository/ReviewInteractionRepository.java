package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.ReviewInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewInteractionRepository extends JpaRepository<ReviewInteraction, Integer> {
    Optional<ReviewInteraction> findByReview_ReviewIdAndUser_UserId(Integer reviewId, Integer userId);
    java.util.List<ReviewInteraction> findByUser_UserId(Integer userId);
}
