package com.cinema.hyperCinema.service.seat;

import com.cinema.hyperCinema.dto.admin.seat.request.SeatGenerateRequest;
import com.cinema.hyperCinema.dto.admin.seat.request.SeatUpdateRequest;
import com.cinema.hyperCinema.dto.admin.seat.response.SeatDetailView;
import com.cinema.hyperCinema.dto.admin.seat.response.SeatMapView;
import com.cinema.hyperCinema.dto.admin.seat.response.ShowtimeSeatView;
import com.cinema.hyperCinema.model.User;

import java.util.List;

public interface SeatService {

    List<SeatDetailView> getSeatsByHall(Integer hallId, User actor);

    SeatMapView getSeatMap(Integer hallId, User actor);

    List<ShowtimeSeatView> getSeatsForShowtime(Integer showtimeId);

    void generateSeats(Integer hallId, SeatGenerateRequest request, User actor);

    void updateSeat(Integer seatId, SeatUpdateRequest request, User actor);

    void updateSeatMaintenance(Integer seatId, String maintenanceStatus, User actor);

    void deleteSeat(Integer seatId, User actor);

    void addSingleSeat(Integer hallId, SeatUpdateRequest request, User actor);

    void clearAllSeats(Integer hallId, User actor);

    void addRow(Integer hallId, String type, User actor);

    void addColumn(Integer hallId, String type, User actor);

    void insertColumnAisle(Integer hallId, Integer afterColumn, User actor);

    void insertRowAisle(Integer hallId, String afterRow, User actor);
}
