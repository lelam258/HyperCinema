package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, Integer> {

    /**
     * Check whether the given branch has at least one Showtime whose
     * start time is strictly after the supplied moment.
     *
     * <p>Used as the guard for the "deactivate branch" flow: a branch cannot
     * be moved to {@code Inactive} while it still has scheduled future
     * showtimes (Requirement 7.4 — error key
     * {@code branch.cannot_deactivate_with_future_showtimes}).
     *
     * <p>The traversal {@code Hall_Branch_BranchId} walks
     * {@link Showtime#getHall()} → {@code Hall.branch} → {@code Branch.branchId}.
     */
    boolean existsByHall_Branch_BranchIdAndStartTimeAfter(Integer branchId, LocalDateTime now);

    boolean existsByMovie_MovieIdAndStartTimeAfter(Integer movieId, LocalDateTime now);

    boolean existsByMovie_MovieId(Integer movieId);

    boolean existsByMovie_MovieIdAndHall_Branch_BranchIdAndStartTimeAfter(
            Integer movieId, Integer branchId, LocalDateTime now);

    long countByMovie_MovieIdAndStartTimeAfter(Integer movieId, LocalDateTime now);

    long countByMovie_MovieIdAndStartTimeLessThanEqual(Integer movieId, LocalDateTime now);

    long countByHall_HallId(Integer hallId);

    boolean existsByHall_HallId(Integer hallId);
}
