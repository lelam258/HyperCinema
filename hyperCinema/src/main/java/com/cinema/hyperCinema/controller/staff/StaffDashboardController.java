package com.cinema.hyperCinema.controller.staff;

import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.model.Showtime;
import com.cinema.hyperCinema.repository.ShowtimeRepository;
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
@RequestMapping("/staff")
public class StaffDashboardController {

    private final ShowtimeRepository showtimeRepository;

    public StaffDashboardController(ShowtimeRepository showtimeRepository) {
        this.showtimeRepository = showtimeRepository;
    }

    @GetMapping({"/dashboard", ""})
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User user = userDetails.getUser();
        
        if (user.getBranch() != null) {
            model.addAttribute("branchName", user.getBranch().getName());
        } else {
            model.addAttribute("branchName", "Chưa phân công chi nhánh");
        }
        
        model.addAttribute("staffName", user.getFullName());

        // ── Meta ──
        model.addAttribute("lastUpdated",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));

        return "staff/dashboard";
    }

    @GetMapping("/booking")
    public String bookingList(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User user = userDetails.getUser();
        
        Integer branchId = user.getBranch() != null ? user.getBranch().getBranchId() : null;
        List<Showtime> showtimes = List.of();
        
        if (branchId != null) {
            showtimes = showtimeRepository.findByHall_Branch_BranchIdAndStartTimeAfterOrderByStartTimeAsc(branchId, LocalDateTime.now());
            model.addAttribute("branchName", user.getBranch().getName());
        } else {
            model.addAttribute("branchName", "Chưa phân công chi nhánh");
        }
        
        model.addAttribute("staffName", user.getFullName());
        model.addAttribute("showtimes", showtimes);
        
        model.addAttribute("lastUpdated",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));
                
        return "staff/booking";
    }
}
