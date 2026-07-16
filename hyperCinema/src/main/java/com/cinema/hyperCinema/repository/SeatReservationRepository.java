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
    @Query("SELECT r.seat.seatId FROM SeatReservation r "
            + "WHERE r.showtime.showtimeId = :showtimeId "
            + "AND r.status = :status "
            + "AND r.expiredAt > :now")
    List<Integer> findActiveReservedSeatIds(@Param("showtimeId") Integer showtimeId,
                                            @Param("status") String status,
                                            @Param("now") LocalDateTime now);

    List<SeatReservation> findByShowtime_ShowtimeIdAndExpiredAtAfter(Integer showtimeId, LocalDateTime now);

    long countByShowtime_ShowtimeId(Integer showtimeId);
}
