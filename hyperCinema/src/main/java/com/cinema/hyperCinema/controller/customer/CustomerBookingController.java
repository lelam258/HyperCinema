package com.cinema.hyperCinema.controller.customer;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.cinema.hyperCinema.security.CustomUserDetails;
import com.cinema.hyperCinema.service.booking.BookingService;
import com.cinema.hyperCinema.service.ui.WorkspaceUiDataService;

@Controller
@RequestMapping("/my/bookings")
public class CustomerBookingController {

    private final BookingService bookingService;
    private final WorkspaceUiDataService workspaceUiDataService;

    public CustomerBookingController(BookingService bookingService,
                                     WorkspaceUiDataService workspaceUiDataService) {
        this.bookingService = bookingService;
        this.workspaceUiDataService = workspaceUiDataService;
    }

    @GetMapping
    public String bookings(@AuthenticationPrincipal CustomUserDetails userDetails,
                           @RequestParam(name = "page", defaultValue = "0") int page,
                           @RequestParam(name = "size", defaultValue = "10") int size,
                           @RequestParam(name = "sort", defaultValue = "createdAt") String sort,
                           @RequestParam(name = "direction", defaultValue = "desc") String direction,
                           Model model) {
        var dashboard = workspaceUiDataService.getCustomerDashboard(userDetails.getUser());
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortField = mapSortField(sort);
        PageRequest pageable = PageRequest.of(Math.max(0, page), normalizeSize(size),
                Sort.by(sortDirection, sortField));
        var bookingPage = bookingService.findBookingsByUser(userDetails.getUser().getUserId(), pageable);
        model.addAttribute("page", bookingPage);
        model.addAttribute("bookings", bookingPage.getContent());
        model.addAttribute("bookingCount", bookingPage.getTotalElements());
        model.addAttribute("sort", sortField);
        model.addAttribute("direction", sortDirection.name().toLowerCase());
        model.addAttribute("size", pageable.getPageSize());
        model.addAttribute("customerName", dashboard.getCustomerName());
        model.addAttribute("membershipTier", dashboard.getMembershipTier());
        model.addAttribute("rewardPoints", dashboard.getRewardPoints());
        model.addAttribute("membershipProgress", dashboard.getMembershipProgress());
        return "my/bookings";
    }

    private int normalizeSize(int size) {
        if (size < 1) {
            return 10;
        }
        return Math.min(size, 50);
    }

    private String mapSortField(String sort) {
        return switch (sort) {
            case "showtime", "showtime.startTime" -> "showtime.startTime";
            case "totalPrice" -> "totalPrice";
            case "status" -> "status";
            case "bookingId" -> "bookingId";
            default -> "createdAt";
        };
    }
}
