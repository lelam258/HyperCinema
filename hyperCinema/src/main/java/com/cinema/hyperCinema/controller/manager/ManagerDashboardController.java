package com.cinema.hyperCinema.controller.manager;

import com.cinema.hyperCinema.service.dashboard.ManagerDashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/manager")
public class ManagerDashboardController {

    private final ManagerDashboardService dashboardService;

    public ManagerDashboardController(ManagerDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping({"/dashboard", ""})
    public String dashboard(Model model) {
        // ── KPI Cards ──
        model.addAttribute("totalRevenue", dashboardService.sumChainRevenueThisMonth());
        model.addAttribute("totalTickets", dashboardService.countChainTicketsThisMonth());
        model.addAttribute("activeBranches", dashboardService.countActiveBranches());
        model.addAttribute("nowShowingMovies", dashboardService.countNowShowingMovies());

        // ── Charts ──
        Map<String, Long> revenueData = dashboardService.getRevenueLastDays(14); // Xem 14 ngày
        model.addAttribute("revenueLabels", revenueData.keySet());
        model.addAttribute("revenueValues", revenueData.values());

        // ── Tables ──
        List<Object[]> branchLeaderboard = dashboardService.getBranchLeaderboardThisMonth();
        model.addAttribute("branchLeaderboard", branchLeaderboard);

        List<Object[]> topMovies = dashboardService.getTopMoviesThisMonth(5);
        model.addAttribute("topMovies", topMovies);

        // ── Meta ──
        model.addAttribute("lastUpdated",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));

        return "manager/dashboard";
    }
}
