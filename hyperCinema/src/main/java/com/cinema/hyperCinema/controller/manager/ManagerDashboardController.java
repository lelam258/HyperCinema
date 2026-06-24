package com.cinema.hyperCinema.controller.manager;

import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.cinema.hyperCinema.dto.ui.admin.MetricCardView;
import com.cinema.hyperCinema.dto.ui.admin.SeriesPointView;
import com.cinema.hyperCinema.dto.ui.workspace.WorkspaceDashboardView;
import com.cinema.hyperCinema.service.ui.WorkspaceUiDataService;

@Controller
@RequestMapping("/manager")
public class ManagerDashboardController {

    private final WorkspaceUiDataService workspaceUiDataService;

    public ManagerDashboardController(WorkspaceUiDataService workspaceUiDataService) {
        this.workspaceUiDataService = workspaceUiDataService;
    }

    @GetMapping({"/dashboard", ""})
    public String dashboard(Model model) {
        WorkspaceDashboardView dashboard = workspaceUiDataService.getManagerDashboard();
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("totalRevenue", metricValue(dashboard, "revenue"));
        model.addAttribute("totalTickets", metricValue(dashboard, "tickets"));
        model.addAttribute("activeBranches", metricValue(dashboard, "branches"));
        model.addAttribute("nowShowingMovies", metricValue(dashboard, "movies"));
        model.addAttribute("revenueLabels", dashboard.getRevenueSeries().stream()
                .map(SeriesPointView::getLabel)
                .collect(Collectors.toList()));
        model.addAttribute("revenueValues", dashboard.getRevenueSeries().stream()
                .map(SeriesPointView::getValue)
                .collect(Collectors.toList()));
        model.addAttribute("branchLeaderboard", dashboard.getLeaderboard().stream()
                .map(row -> new Object[] {row.getLabel(), row.getValue()})
                .collect(Collectors.toList()));
        model.addAttribute("topMovies", dashboard.getTopMovies().stream()
                .map(movie -> new Object[] {movie.getTitle(), movie.getBookingCount()})
                .collect(Collectors.toList()));
        model.addAttribute("lastUpdated", dashboard.getLastUpdated());
        return "manager/dashboard";
    }

    private long metricValue(WorkspaceDashboardView dashboard, String key) {
        return dashboard.getMetrics().stream()
                .filter(metric -> key.equals(metric.getKey()))
                .findFirst()
                .map(MetricCardView::getValue)
                .orElse(0L);
    }
}
