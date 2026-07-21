package com.cinema.hyperCinema.controller.customer;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cinema.hyperCinema.dto.booking.CustomerBookingHistoryFilter;
import com.cinema.hyperCinema.exception.booking.BookingManagementException;
import com.cinema.hyperCinema.security.CustomUserDetails;
import com.cinema.hyperCinema.service.booking.BookingManagementService;
import com.cinema.hyperCinema.service.booking.BookingService;
import com.cinema.hyperCinema.service.ui.WorkspaceUiDataService;

@Controller
@RequestMapping("/my/bookings")
public class CustomerBookingController {

    private final BookingService bookingService;
    private final BookingManagementService bookingManagementService;
    private final WorkspaceUiDataService workspaceUiDataService;

    public CustomerBookingController(BookingService bookingService,
                                     BookingManagementService bookingManagementService,
                                     WorkspaceUiDataService workspaceUiDataService) {
        this.bookingService = bookingService;
        this.bookingManagementService = bookingManagementService;
        this.workspaceUiDataService = workspaceUiDataService;
    }

    @GetMapping
    public String bookings(@AuthenticationPrincipal CustomUserDetails userDetails,
                           @ModelAttribute("filter") CustomerBookingHistoryFilter filter,
                           @RequestParam(name = "page", defaultValue = "0") int page,
                           Model model) {
        var dashboard = workspaceUiDataService.getCustomerDashboard(userDetails.getUser());
        CustomerBookingHistoryFilter safeFilter = filter != null ? filter : new CustomerBookingHistoryFilter();
        safeFilter.normalize();
        PageRequest pageable = PageRequest.of(Math.max(0, page), safeFilter.pageSize(), safeFilter.toSort());
        var bookingPage = bookingService.findBookingsByUser(userDetails.getUser().getUserId(), safeFilter, pageable);
        model.addAttribute("page", bookingPage);
        model.addAttribute("bookings", bookingPage.getContent());
        model.addAttribute("bookingCount", bookingPage.getTotalElements());
        model.addAttribute("filter", safeFilter);
        model.addAttribute("sort", safeFilter.getSort());
        model.addAttribute("direction", safeFilter.getDirection());
        model.addAttribute("size", pageable.getPageSize());
        model.addAttribute("customerName", dashboard.getCustomerName());
        model.addAttribute("membershipTier", dashboard.getMembershipTier());
        model.addAttribute("rewardPoints", dashboard.getRewardPoints());
        model.addAttribute("membershipProgress", dashboard.getMembershipProgress());
        return "my/bookings";
    }

    @GetMapping("/{bookingId}")
    public String bookingDetail(@AuthenticationPrincipal CustomUserDetails userDetails,
                                @PathVariable Integer bookingId,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        try {
            var booking = bookingManagementService.findCustomerDetail(userDetails.getUser(), bookingId);
            var dashboard = workspaceUiDataService.getCustomerDashboard(userDetails.getUser());
            model.addAttribute("booking", booking);
            model.addAttribute("customerName", dashboard.getCustomerName());
            model.addAttribute("membershipTier", dashboard.getMembershipTier());
            model.addAttribute("rewardPoints", dashboard.getRewardPoints());
            model.addAttribute("membershipProgress", dashboard.getMembershipProgress());
            return "my/booking-detail";
        } catch (BookingManagementException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessageKey());
            return "redirect:/my/bookings";
        }
    }

}
