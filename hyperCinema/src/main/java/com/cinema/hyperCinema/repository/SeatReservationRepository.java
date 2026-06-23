package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.SeatReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SeatReservationRepository extends JpaRepository<SeatReservation, Integer> {

    List<SeatReservation> findByShowtime_ShowtimeIdAndExpiredAtAfter(Integer showtimeId, LocalDateTime now);
}
