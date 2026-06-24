package com.cinema.hyperCinema.controller.ui;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cinema.hyperCinema.dto.ui.booking.FoodAddonOptionView;
import com.cinema.hyperCinema.dto.ui.booking.SeatAvailabilityView;
import com.cinema.hyperCinema.dto.ui.booking.ShowtimeOptionView;
import com.cinema.hyperCinema.dto.ui.booking.VoucherPreviewView;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.security.CustomUserDetails;
import com.cinema.hyperCinema.service.ui.BookingUiDataService;

@RestController
@RequestMapping("/api/ui")
public class BookingUiDataController {

    private final BookingUiDataService bookingUiDataService;

    public BookingUiDataController(BookingUiDataService bookingUiDataService) {
        this.bookingUiDataService = bookingUiDataService;
    }

    @GetMapping("/showtimes")
    public List<ShowtimeOptionView> showtimes(@AuthenticationPrincipal CustomUserDetails principal,
                                              @RequestParam(defaultValue = "20") int limit) {
        return bookingUiDataService.upcomingShowtimes(actor(principal), Math.min(Math.max(limit, 1), 50));
    }

    @GetMapping("/showtimes/{showtimeId}/seats")
    public List<SeatAvailabilityView> seats(@PathVariable Integer showtimeId,
                                            @AuthenticationPrincipal CustomUserDetails principal) {
        return bookingUiDataService.seatAvailability(showtimeId, actor(principal));
    }

    @GetMapping("/food-items")
    public List<FoodAddonOptionView> foodItems(@AuthenticationPrincipal CustomUserDetails principal) {
        return bookingUiDataService.availableFoodItems(actor(principal));
    }

    @GetMapping("/vouchers/preview")
    public VoucherPreviewView voucherPreview(@RequestParam String code,
                                             @RequestParam long orderValue,
                                             @RequestParam(required = false) Integer branchId) {
        return bookingUiDataService.previewVoucher(code, orderValue, branchId);
    }

    private User actor(CustomUserDetails principal) {
        return principal != null ? principal.getUser() : null;
    }
}
