package com.cinema.hyperCinema.controller.admin;

import com.cinema.hyperCinema.service.dashboard.AdminDashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Controller cho Admin Dashboard — tổng quan toàn hệ thống.
 */
@Controller
@RequestMapping("/admin")
public class DashboardController {

    private final AdminDashboardService dashboardService;

    public DashboardController(AdminDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping({"/dashboard", ""})
    public String dashboard(Model model) {
        // ── KPI Cards ──
        model.addAttribute("totalUsers", dashboardService.countTotalUsers());
        model.addAttribute("activeUsers", dashboardService.countActiveUsers());
        model.addAttribute("totalBranches", dashboardService.countTotalBranches());
        model.addAttribute("activeBranches", dashboardService.countActiveBranches());
        model.addAttribute("totalMovies", dashboardService.countTotalMovies());
        model.addAttribute("nowShowingMovies", dashboardService.countNowShowingMovies());
        model.addAttribute("todayBookings", dashboardService.countTodayBookings());
        model.addAttribute("todayRevenue", dashboardService.sumTodayRevenue());

        // ── Charts ──
        Map<String, Long> revenueData = dashboardService.getRevenueLastDays(7);
        model.addAttribute("revenueLabels", revenueData.keySet());
        model.addAttribute("revenueValues", revenueData.values());

        Map<String, Long> userDist = dashboardService.getUserDistributionByRole();
        model.addAttribute("roleLabels", userDist.keySet());
        model.addAttribute("roleValues", userDist.values());

        // ── Tables ──
        List<Object[]> topMovies = dashboardService.getTopMoviesByBookingCount(5);
        model.addAttribute("topMovies", topMovies);

        List<Object[]> recentLogs = dashboardService.getRecentAuditLogs(10);
        model.addAttribute("recentLogs", recentLogs);

        // ── Meta ──
        model.addAttribute("lastUpdated",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));

        return "admin/dashboard";
    }
}
