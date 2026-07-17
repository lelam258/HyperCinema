package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Integer> {

    long countByHall_HallId(Integer hallId);

    boolean existsByHall_HallId(Integer hallId);

    @Modifying
    @Query("UPDATE Seat s SET s.maintenanceStatus = :maintenanceStatus WHERE s.hall.hallId = :hallId")
    int updateMaintenanceStatusByHallId(
            @Param("hallId") Integer hallId,
            @Param("maintenanceStatus") String maintenanceStatus);

    // Seat map: all seats in a hall, ordered by row then number
    List<Seat> findByHall_HallIdOrderBySeatRowAscSeatNumberAsc(Integer hallId);

    // Duplicate check: single seat
    boolean existsByHall_HallIdAndSeatRowAndSeatNumber(Integer hallId, String seatRow, Integer seatNumber);

    // Duplicate check: excluding self (for update)
    boolean existsByHall_HallIdAndSeatRowAndSeatNumberAndSeatIdNot(
            Integer hallId, String seatRow, Integer seatNumber, Integer seatId);

    // Bulk duplicate check: find seats matching any (row, number) in given hall
    @Query("SELECT s FROM Seat s WHERE s.hall.hallId = :hallId " +
           "AND CONCAT(s.seatRow, '-', s.seatNumber) IN :keys")
    List<Seat> findByHallIdAndRowNumberKeys(
            @Param("hallId") Integer hallId,
            @Param("keys") Collection<String> keys);
}
