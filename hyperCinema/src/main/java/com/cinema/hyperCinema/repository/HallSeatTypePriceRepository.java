package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.HallSeatTypePrice;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HallSeatTypePriceRepository extends JpaRepository<HallSeatTypePrice, Integer> {

    List<HallSeatTypePrice> findByHall_HallIdOrderBySeatTypeAsc(Integer hallId);

    Optional<HallSeatTypePrice> findByHall_HallIdAndSeatTypeAndActiveTrue(Integer hallId, String seatType);
}
