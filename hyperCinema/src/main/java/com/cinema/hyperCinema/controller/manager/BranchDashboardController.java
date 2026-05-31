package com.cinema.hyperCinema.controller.manager;

import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.security.CustomUserDetails;
import com.cinema.hyperCinema.service.dashboard.BranchManagerDashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/branch")
public class BranchDashboardController {

    private final BranchManagerDashboardService dashboardService;

    public BranchDashboardController(BranchManagerDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping({"/dashboard", ""})
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User user = userDetails.getUser();
        Integer branchId = null;

        if (user.getBranch() != null) {
            branchId = user.getBranch().getBranchId();
            model.addAttribute("branchName", user.getBranch().getName());
        } else {
            // Fallback for demo or edge case
            branchId = 1;
            model.addAttribute("branchName", "Chi nhánh 1");
        }

        // ── KPI Cards ──
        model.addAttribute("branchRevenue", dashboardService.sumBranchRevenueThisMonth(branchId));
        model.addAttribute("branchTickets", dashboardService.countBranchTicketsThisMonth(branchId));
        model.addAttribute("todayBookings", dashboardService.countBranchBookingsToday(branchId));

        // ── Charts ──
        Map<String, Long> revenueData = dashboardService.getBranchRevenueLastDays(branchId, 14);
        model.addAttribute("revenueLabels", revenueData.keySet());
        model.addAttribute("revenueValues", revenueData.values());

        // ── Tables ──
        List<Object[]> topMovies = dashboardService.getBranchTopMovies(branchId, 5);
        model.addAttribute("topMovies", topMovies);

        // ── Meta ──
        model.addAttribute("lastUpdated",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));

        return "branch/dashboard";
    }
}
