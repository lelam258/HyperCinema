package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.SeatReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SeatReservationRepository extends JpaRepository<SeatReservation, Integer> {

    /**
     * Check if any reservation exists for the given seat with the specified status
     * and expiration time after the provided timestamp.
     * Used for active reference check before seat deletion
     * (e.g., status = 'ACTIVE' and expiredAt > now).
     */
    boolean existsBySeat_SeatIdAndStatusAndExpiredAtAfter(Integer seatId, String status, LocalDateTime now);

    @Query("SELECT r.seat.seatId FROM SeatReservation r "
            + "WHERE r.showtime.showtimeId = :showtimeId "
            + "AND r.status = :status "
            + "AND r.expiredAt > :now")
    List<Integer> findActiveReservedSeatIds(@Param("showtimeId") Integer showtimeId,
                                            @Param("status") String status,
                                            @Param("now") LocalDateTime now);

    long countByShowtime_ShowtimeId(Integer showtimeId);
}
