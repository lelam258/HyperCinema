package com.cinema.hyperCinema.controller.admin;

import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.cinema.hyperCinema.dto.ui.admin.AdminDashboardView;
import com.cinema.hyperCinema.dto.ui.admin.MetricCardView;
import com.cinema.hyperCinema.dto.ui.admin.SeriesPointView;
import com.cinema.hyperCinema.service.ui.AdminUiDataService;

@Controller
@RequestMapping("/admin")
public class DashboardController {

    private final AdminUiDataService adminUiDataService;

    public DashboardController(AdminUiDataService adminUiDataService) {
        this.adminUiDataService = adminUiDataService;
    }

    @GetMapping({"/dashboard", ""})
    public String dashboard(Model model) {
        AdminDashboardView dashboard = adminUiDataService.getDashboard();
        model.addAttribute("dashboard", dashboard);
        addCompatibilityAttributes(model, dashboard);
        return "admin/dashboard";
    }

    private void addCompatibilityAttributes(Model model, AdminDashboardView dashboard) {
        model.addAttribute("totalUsers", metricValue(dashboard, "users"));
        model.addAttribute("activeUsers", metricHelperNumber(dashboard, "users"));
        model.addAttribute("totalBranches", metricValue(dashboard, "branches"));
        model.addAttribute("activeBranches", metricHelperNumber(dashboard, "branches"));
        model.addAttribute("totalMovies", metricValue(dashboard, "movies"));
        model.addAttribute("nowShowingMovies", metricHelperNumber(dashboard, "movies"));
        model.addAttribute("todayBookings", metricValue(dashboard, "today"));
        model.addAttribute("todayRevenue", metricHelperDigits(dashboard, "today"));
        model.addAttribute("revenueLabels", dashboard.getRevenueSeries().stream()
                .map(SeriesPointView::getLabel)
                .collect(Collectors.toList()));
        model.addAttribute("revenueValues", dashboard.getRevenueSeries().stream()
                .map(SeriesPointView::getValue)
                .collect(Collectors.toList()));
        model.addAttribute("roleLabels", dashboard.getRoleDistribution().stream()
                .map(SeriesPointView::getLabel)
                .collect(Collectors.toList()));
        model.addAttribute("roleValues", dashboard.getRoleDistribution().stream()
                .map(SeriesPointView::getValue)
                .collect(Collectors.toList()));
        model.addAttribute("topMovies", dashboard.getTopMovies());
        model.addAttribute("recentLogs", dashboard.getRecentLogs());
        model.addAttribute("lastUpdated", dashboard.getLastUpdated());
    }

    private long metricValue(AdminDashboardView dashboard, String key) {
        return dashboard.getMetrics().stream()
                .filter(metric -> key.equals(metric.getKey()))
                .findFirst()
                .map(MetricCardView::getValue)
                .orElse(0L);
    }

    private String metricHelperText(AdminDashboardView dashboard, String key) {
        return dashboard.getMetrics().stream()
                .filter(metric -> key.equals(metric.getKey()))
                .findFirst()
                .map(MetricCardView::getHelperText)
                .orElse("0");
    }

    private long metricHelperNumber(AdminDashboardView dashboard, String key) {
        String helperText = metricHelperText(dashboard, key);
        String digits = helperText.replaceAll("[^0-9]", " ").trim();
        if (digits.isEmpty()) {
            return 0L;
        }
        return Long.parseLong(digits.split("\\s+")[0]);
    }

    private long metricHelperDigits(AdminDashboardView dashboard, String key) {
        String digits = metricHelperText(dashboard, key).replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return 0L;
        }
        return Long.parseLong(digits);
    }
}
