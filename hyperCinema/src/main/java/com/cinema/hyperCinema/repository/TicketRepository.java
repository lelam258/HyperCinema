package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Integer> {

    /**
     * Check if any ticket exists for the given seat with a status different from the provided one.
     * Used for active reference check before seat deletion (e.g., status != 'CANCELLED').
     */
    boolean existsBySeat_SeatIdAndStatusNot(Integer seatId, String status);

    List<Ticket> findByBooking_Showtime_ShowtimeIdAndBooking_StatusNot(Integer showtimeId, String status);

    @Query("SELECT t.seat.seatId FROM Ticket t "
            + "WHERE t.booking.showtime.showtimeId = :showtimeId "
            + "AND t.status NOT IN :cancelledStatuses")
    List<Integer> findUnavailableSeatIdsByShowtimeId(@Param("showtimeId") Integer showtimeId,
                                                     @Param("cancelledStatuses") List<String> cancelledStatuses);

    long countByBooking_Showtime_ShowtimeId(Integer showtimeId);
}
