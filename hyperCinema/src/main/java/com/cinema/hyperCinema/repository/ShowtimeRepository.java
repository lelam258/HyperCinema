package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.Showtime;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    List<Showtime> findByStartTimeAfterOrderByStartTimeAsc(LocalDateTime startTime, Pageable pageable);

    List<Showtime> findByHall_Branch_BranchIdAndStartTimeAfterOrderByStartTimeAsc(
            Integer branchId, LocalDateTime startTime, Pageable pageable);

    @Query("""
            SELECT s
            FROM Showtime s
            JOIN FETCH s.movie m
            JOIN FETCH s.hall h
            JOIN FETCH h.branch b
            WHERE m.movieId = :movieId
              AND s.startTime >= :startTime
            ORDER BY s.startTime ASC
            """)
    List<Showtime> findUpcomingByMovieIdWithHallAndBranch(
            @Param("movieId") Integer movieId,
            @Param("startTime") LocalDateTime startTime);

    @Query("""
            SELECT s
            FROM Showtime s
            JOIN FETCH s.movie m
            JOIN FETCH s.hall h
            JOIN FETCH h.branch b
            WHERE s.showtimeId = :showtimeId
            """)
    Optional<Showtime> findByIdWithMovieHallAndBranch(@Param("showtimeId") Integer showtimeId);
}
