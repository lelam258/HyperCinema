package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.Booking;
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
}
