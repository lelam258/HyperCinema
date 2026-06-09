package com.cinema.hyperCinema.controller.customer;

import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.model.Booking;
import com.cinema.hyperCinema.repository.BookingRepository;
import com.cinema.hyperCinema.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/my")
public class CustomerDashboardController {

    private final BookingRepository bookingRepository;

    public CustomerDashboardController(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @GetMapping({"/dashboard", ""})
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User user = userDetails.getUser();
        
        model.addAttribute("customerName", user.getFullName());
        model.addAttribute("email", user.getEmail());
        model.addAttribute("phone", user.getPhone());
        
        // Mock data cho Customer (điểm thưởng, hạng thành viên)
        model.addAttribute("rewardPoints", 1250);
        model.addAttribute("membershipTier", "Vàng");

        // ── Meta ──
        model.addAttribute("lastUpdated",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));

        return "my/dashboard";
    }

    @GetMapping("/bookings")
    public String bookingHistory(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User user = userDetails.getUser();
        List<Booking> bookings = bookingRepository.findByUser_UserIdOrderByCreatedAtDesc(user.getUserId());
        
        model.addAttribute("customerName", user.getFullName());
        model.addAttribute("bookings", bookings);
        
        model.addAttribute("lastUpdated",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));
                
        return "my/bookings";
    }
}
