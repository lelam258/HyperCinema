package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.Hall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HallRepository extends JpaRepository<Hall, Integer> {
}
