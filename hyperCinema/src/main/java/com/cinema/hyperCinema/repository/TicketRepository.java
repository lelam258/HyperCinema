package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Integer> {

    List<Ticket> findByBooking_Showtime_ShowtimeIdAndBooking_StatusNot(Integer showtimeId, String status);
}
