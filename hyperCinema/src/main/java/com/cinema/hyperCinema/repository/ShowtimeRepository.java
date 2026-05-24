package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, Integer> {
}
