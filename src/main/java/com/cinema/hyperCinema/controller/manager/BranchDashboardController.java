package com.cinema.hyperCinema.controller.manager;

import java.util.stream.Collectors;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.cinema.hyperCinema.dto.ui.admin.MetricCardView;
import com.cinema.hyperCinema.dto.ui.admin.SeriesPointView;
import com.cinema.hyperCinema.dto.ui.workspace.WorkspaceDashboardView;
import com.cinema.hyperCinema.security.CustomUserDetails;
import com.cinema.hyperCinema.service.ui.WorkspaceUiDataService;

@Controller
@RequestMapping("/branch")
public class BranchDashboardController {

    private final WorkspaceUiDataService workspaceUiDataService;

    public BranchDashboardController(WorkspaceUiDataService workspaceUiDataService) {
        this.workspaceUiDataService = workspaceUiDataService;
    }

    @GetMapping({"/dashboard", ""})
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        WorkspaceDashboardView dashboard = workspaceUiDataService.getBranchDashboard(userDetails.getUser());
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("branchName", dashboard.getBranchName());
        model.addAttribute("branchRevenue", metricValue(dashboard, "revenue"));
        model.addAttribute("branchTickets", metricValue(dashboard, "tickets"));
        model.addAttribute("todayBookings", metricValue(dashboard, "todayBookings"));
        model.addAttribute("revenueLabels", dashboard.getRevenueSeries().stream()
                .map(SeriesPointView::getLabel)
                .collect(Collectors.toList()));
        model.addAttribute("revenueValues", dashboard.getRevenueSeries().stream()
                .map(SeriesPointView::getValue)
                .collect(Collectors.toList()));
        model.addAttribute("topMovies", dashboard.getTopMovies().stream()
                .map(movie -> new Object[] {movie.getTitle(), movie.getBookingCount()})
                .collect(Collectors.toList()));
        model.addAttribute("lastUpdated", dashboard.getLastUpdated());
        return "branch/dashboard";
    }

    private long metricValue(WorkspaceDashboardView dashboard, String key) {
        return dashboard.getMetrics().stream()
                .filter(metric -> key.equals(metric.getKey()))
                .findFirst()
                .map(MetricCardView::getValue)
                .orElse(0L);
    }
}
