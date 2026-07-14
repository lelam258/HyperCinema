package com.cinema.hyperCinema.service.ui;

import com.cinema.hyperCinema.dto.ui.booking.*;
import com.cinema.hyperCinema.model.User;

import java.util.List;

public interface BookingUiDataService {

    List<ShowtimeOptionView> upcomingShowtimes(User actor, int limit);

    List<SeatAvailabilityView> seatAvailability(Integer showtimeId, User actor);

    List<FoodAddonOptionView> availableFoodItems(User actor);

    VoucherPreviewView previewVoucher(String code, long orderValue, Integer branchId);

    PosSummaryView emptyPosSummary(User actor);
}
