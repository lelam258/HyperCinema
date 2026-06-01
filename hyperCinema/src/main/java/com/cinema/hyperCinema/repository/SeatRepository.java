package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Integer> {

    long countByHall_HallId(Integer hallId);

    boolean existsByHall_HallId(Integer hallId);
}
