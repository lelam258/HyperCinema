package com.cinema.hyperCinema.service.ui;

import java.util.List;

import com.cinema.hyperCinema.dto.ui.booking.FoodAddonOptionView;
import com.cinema.hyperCinema.dto.ui.booking.PosSummaryView;
import com.cinema.hyperCinema.dto.ui.booking.SeatAvailabilityView;
import com.cinema.hyperCinema.dto.ui.booking.ShowtimeOptionView;
import com.cinema.hyperCinema.dto.ui.booking.VoucherPreviewView;
import com.cinema.hyperCinema.model.User;

public interface BookingUiDataService {

    List<ShowtimeOptionView> upcomingShowtimes(User actor, int limit);

    List<SeatAvailabilityView> seatAvailability(Integer showtimeId, User actor);

    List<FoodAddonOptionView> availableFoodItems(User actor);

    VoucherPreviewView previewVoucher(String code, long orderValue, Integer branchId);

    PosSummaryView emptyPosSummary(User actor);
}
