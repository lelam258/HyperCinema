package com.cinema.hyperCinema.controller.customer;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.cinema.hyperCinema.repository.BookingRepository;
import com.cinema.hyperCinema.security.CustomUserDetails;
import com.cinema.hyperCinema.service.ui.WorkspaceUiDataService;

@Controller
@RequestMapping("/my/bookings")
public class CustomerBookingController {

    private final BookingRepository bookingRepository;
    private final WorkspaceUiDataService workspaceUiDataService;

    public CustomerBookingController(BookingRepository bookingRepository,
                                     WorkspaceUiDataService workspaceUiDataService) {
        this.bookingRepository = bookingRepository;
        this.workspaceUiDataService = workspaceUiDataService;
    }

    @GetMapping
    public String bookings(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        var dashboard = workspaceUiDataService.getCustomerDashboard(userDetails.getUser());
        var bookings = bookingRepository.findByUser_UserIdOrderByCreatedAtDesc(
                userDetails.getUser().getUserId(), PageRequest.of(0, 50));
        model.addAttribute("bookings", bookings);
        model.addAttribute("bookingCount", bookings.size());
        model.addAttribute("customerName", dashboard.getCustomerName());
        model.addAttribute("membershipTier", dashboard.getMembershipTier());
        model.addAttribute("rewardPoints", dashboard.getRewardPoints());
        return "my/bookings";
    }
}
