package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.SeatReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SeatReservationRepository extends JpaRepository<SeatReservation, Integer> {
}
