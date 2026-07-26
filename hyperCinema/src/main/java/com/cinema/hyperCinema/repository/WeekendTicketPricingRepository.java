package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.WeekendTicketPricing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WeekendTicketPricingRepository extends JpaRepository<WeekendTicketPricing, Integer> {

    List<WeekendTicketPricing> findByOrderByHall_NameAsc();

    Optional<WeekendTicketPricing> findByHall_HallId(Integer hallId);

    Optional<WeekendTicketPricing> findByHall_HallIdAndActiveTrue(Integer hallId);
}
