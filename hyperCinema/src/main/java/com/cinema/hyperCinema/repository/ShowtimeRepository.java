package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.Showtime;
import org.springframework.data.domain.Page;
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
    @Query("""
            SELECT COUNT(s) > 0
            FROM Showtime s
            WHERE s.hall.branch.branchId = :branchId
              AND s.startTime > :now
              AND s.status = 'ACTIVE'
            """)
    boolean existsByHall_Branch_BranchIdAndStartTimeAfter(
            @Param("branchId") Integer branchId,
            @Param("now") LocalDateTime now);

    @Query("""
            SELECT COUNT(s) > 0
            FROM Showtime s
            WHERE s.movie.movieId = :movieId
              AND s.startTime > :now
              AND s.status = 'ACTIVE'
            """)
    boolean existsByMovie_MovieIdAndStartTimeAfter(
            @Param("movieId") Integer movieId,
            @Param("now") LocalDateTime now);

    boolean existsByMovie_MovieId(Integer movieId);

    @Query("""
            SELECT COUNT(s) > 0
            FROM Showtime s
            WHERE s.movie.movieId = :movieId
              AND s.hall.branch.branchId = :branchId
              AND s.startTime > :now
              AND s.status = 'ACTIVE'
            """)
    boolean existsByMovie_MovieIdAndHall_Branch_BranchIdAndStartTimeAfter(
            @Param("movieId") Integer movieId,
            @Param("branchId") Integer branchId,
            @Param("now") LocalDateTime now);

    long countByMovie_MovieIdAndStartTimeAfter(Integer movieId, LocalDateTime now);

    long countByMovie_MovieIdAndStartTimeLessThanEqual(Integer movieId, LocalDateTime now);

    long countByHall_HallId(Integer hallId);

    @Query("""
            SELECT COUNT(s) > 0
            FROM Showtime s
            WHERE s.hall.hallId = :hallId
              AND s.status = 'ACTIVE'
            """)
    boolean existsByHall_HallId(@Param("hallId") Integer hallId);

    @Query("""
            SELECT COUNT(s) > 0
            FROM Showtime s
            WHERE s.hall.hallId = :hallId
              AND s.startTime > :now
              AND s.status = 'ACTIVE'
            """)
    boolean existsByHall_HallIdAndStartTimeAfter(
            @Param("hallId") Integer hallId,
            @Param("now") LocalDateTime now);

    @Query("""
            SELECT s
            FROM Showtime s
            WHERE s.startTime > :startTime
              AND s.status = 'ACTIVE'
            ORDER BY s.startTime ASC
            """)
    List<Showtime> findByStartTimeAfterOrderByStartTimeAsc(
            @Param("startTime") LocalDateTime startTime,
            Pageable pageable);

    @Query("""
            SELECT s
            FROM Showtime s
            WHERE s.hall.branch.branchId = :branchId
              AND s.startTime > :startTime
              AND s.status = 'ACTIVE'
            ORDER BY s.startTime ASC
            """)
    default List<Showtime> findByHall_Branch_BranchIdAndStartTimeAfterOrderByStartTimeAsc(
            Integer branchId,
            LocalDateTime startTime) {
        return findByHall_Branch_BranchIdAndStartTimeAfterOrderByStartTimeAsc(
                branchId,
                startTime,
                Pageable.unpaged());
    }

    List<Showtime> findByHall_Branch_BranchIdAndStartTimeAfterOrderByStartTimeAsc(
            @Param("branchId") Integer branchId,
            @Param("startTime") LocalDateTime startTime,
            Pageable pageable);

    @Query("""
            SELECT s
            FROM Showtime s
            JOIN FETCH s.movie m
            JOIN FETCH s.hall h
            JOIN FETCH h.branch b
            WHERE m.movieId = :movieId
              AND s.startTime >= :startTime
              AND s.status = 'ACTIVE'
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

    @Query("""
            SELECT s
            FROM Showtime s
            JOIN FETCH s.movie m
            JOIN FETCH s.hall h
            JOIN FETCH h.branch b
            WHERE (:keyword IS NULL OR LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:movieId IS NULL OR m.movieId = :movieId)
              AND (:branchId IS NULL OR b.branchId = :branchId)
              AND (:hallId IS NULL OR h.hallId = :hallId)
              AND (:startFrom IS NULL OR s.startTime >= :startFrom)
              AND (:startTo IS NULL OR s.startTime <= :startTo)
              AND s.status = 'ACTIVE'
            """)
    Page<Showtime> searchManaged(
            @Param("keyword") String keyword,
            @Param("movieId") Integer movieId,
            @Param("branchId") Integer branchId,
            @Param("hallId") Integer hallId,
            @Param("startFrom") LocalDateTime startFrom,
            @Param("startTo") LocalDateTime startTo,
            Pageable pageable);

    @Query("""
            SELECT COUNT(s) > 0
            FROM Showtime s
            WHERE s.hall.hallId = :hallId
              AND s.status = 'ACTIVE'
              AND s.startTime < :endTime
              AND s.endTime > :startTime
            """)
    boolean existsOverlap(
            @Param("hallId") Integer hallId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("""
            SELECT COUNT(s) > 0
            FROM Showtime s
            WHERE s.hall.hallId = :hallId
              AND s.showtimeId <> :showtimeId
              AND s.status = 'ACTIVE'
              AND s.startTime < :endTime
              AND s.endTime > :startTime
            """)
    boolean existsOverlapExcludingShowtime(
            @Param("hallId") Integer hallId,
            @Param("showtimeId") Integer showtimeId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
