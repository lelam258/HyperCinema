package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Integer> {

    List<Booking> findByUser_UserIdOrderByCreatedAtDesc(Integer userId);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Top N phim theo số booking (không tính Cancelled).
     * Returns: [movieTitle, bookingCount]
     */
    @Query(value = "SELECT m.title, COUNT(b) AS cnt "
            + "FROM Booking b JOIN b.showtime s JOIN s.movie m "
            + "WHERE b.status <> 'Cancelled' "
            + "GROUP BY m.title ORDER BY cnt DESC LIMIT :limit")
    List<Object[]> findTopMoviesByBookingCount(@Param("limit") int limit);

    /**
     * Tổng số vé bán (không tính Cancelled).
     */
    @Query("SELECT COUNT(t) FROM Ticket t JOIN t.booking b "
            + "WHERE b.status <> 'Cancelled' AND b.createdAt BETWEEN :start AND :end")
    long countTicketsByBookingStatusAndCreatedAtBetween(@Param("start") LocalDateTime start,
                                                        @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(b) FROM Booking b JOIN b.showtime s JOIN s.hall h "
            + "WHERE h.branch.branchId = :branchId "
            + "AND b.createdAt BETWEEN :start AND :end")
    long countBookingsByBranchIdAndDateRange(@Param("branchId") Integer branchId,
                                             @Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(t) FROM Ticket t JOIN t.booking b JOIN b.showtime s JOIN s.hall h "
            + "WHERE h.branch.branchId = :branchId "
            + "AND b.status <> 'Cancelled' "
            + "AND b.createdAt BETWEEN :start AND :end")
    long countTicketsByBranchIdAndDateRange(@Param("branchId") Integer branchId,
                                            @Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);

    @Query(value = "SELECT m.title, COUNT(b) AS cnt "
            + "FROM Booking b JOIN b.showtime s JOIN s.movie m JOIN s.hall h "
            + "WHERE h.branch.branchId = :branchId "
            + "AND b.status <> 'Cancelled' "
            + "GROUP BY m.title ORDER BY cnt DESC LIMIT :limit")
    List<Object[]> findTopMoviesByBranchId(@Param("branchId") Integer branchId,
                                           @Param("limit") int limit);

    Page<Booking> findByUser_UserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    boolean existsByPromotion_PromotionIdAndStatusNot(Integer promotionId, String status);

    @Query("SELECT COUNT(b) > 0 FROM Booking b " +
           "WHERE b.user.userId = :userId " +
           "AND b.showtime.movie.movieId = :movieId " +
           "AND (b.status = 'Confirmed' OR b.status = 'Completed')")
    boolean hasSuccessfulBookingForMovie(@Param("userId") Integer userId, @Param("movieId") Integer movieId);

    @Query("SELECT b FROM Booking b " +
           "JOIN FETCH b.showtime s " +
           "JOIN FETCH s.movie m " +
           "JOIN FETCH s.hall h " +
           "JOIN FETCH h.branch br " +
           "WHERE b.user.userId = :userId AND (b.status = 'Confirmed' OR b.status = 'Completed') " +
           "ORDER BY b.createdAt DESC")
    List<Booking> findSuccessfulBookingsByUser(@Param("userId") Integer userId);

    @Query("SELECT COUNT(b) > 0 FROM Booking b " +
           "WHERE b.user.userId = :userId " +
           "AND b.showtime.movie.movieId = :movieId " +
           "AND (b.status = 'Confirmed' OR b.status = 'Completed') " +
           "AND b.showtime.endTime < :now")
    boolean hasEndedSuccessfulBookingForMovie(
            @Param("userId") Integer userId, 
            @Param("movieId") Integer movieId, 
            @Param("now") LocalDateTime now);
}

