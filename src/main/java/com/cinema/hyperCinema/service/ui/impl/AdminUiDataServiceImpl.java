package com.cinema.hyperCinema.service.ui.impl;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.cinema.hyperCinema.dto.ui.admin.ActivityLogView;
import com.cinema.hyperCinema.dto.ui.admin.AdminDashboardView;
import com.cinema.hyperCinema.dto.ui.admin.MetricCardView;
import com.cinema.hyperCinema.dto.ui.admin.QuickActionView;
import com.cinema.hyperCinema.dto.ui.admin.SeriesPointView;
import com.cinema.hyperCinema.dto.ui.admin.TopMovieView;
import com.cinema.hyperCinema.service.dashboard.AdminDashboardService;
import com.cinema.hyperCinema.service.ui.AdminUiDataService;
import com.cinema.hyperCinema.util.UiDisplayMapper;

@Service
public class AdminUiDataServiceImpl implements AdminUiDataService {

    private final AdminDashboardService dashboardService;
    private final UiDisplayMapper displayMapper;

    public AdminUiDataServiceImpl(AdminDashboardService dashboardService, UiDisplayMapper displayMapper) {
        this.dashboardService = dashboardService;
        this.displayMapper = displayMapper;
    }

    @Override
    public AdminDashboardView getDashboard() {
        long totalUsers = dashboardService.countTotalUsers();
        long activeUsers = dashboardService.countActiveUsers();
        long totalBranches = dashboardService.countTotalBranches();
        long activeBranches = dashboardService.countActiveBranches();
        long totalMovies = dashboardService.countTotalMovies();
        long nowShowingMovies = dashboardService.countNowShowingMovies();
        long todayBookings = dashboardService.countTodayBookings();
        long todayRevenue = dashboardService.sumTodayRevenue();

        return AdminDashboardView.builder()
                .metrics(Arrays.asList(
                        metric("users", "Nguoi dung", totalUsers,
                                activeUsers + " dang hoat dong", "users"),
                        metric("branches", "Chi nhanh", totalBranches,
                                activeBranches + " dang mo", "building-2"),
                        metric("movies", "Phim", totalMovies,
                                nowShowingMovies + " dang chieu", "film"),
                        metric("today", "Hom nay", todayBookings,
                                displayMapper.currency(todayRevenue), "ticket-check")))
                .revenueSeries(series(dashboardService.getRevenueLastDays(7), true))
                .roleDistribution(series(dashboardService.getUserDistributionByRole(), false))
                .topMovies(topMovies(dashboardService.getTopMoviesByBookingCount(5)))
                .recentLogs(recentLogs(dashboardService.getRecentAuditLogs(10)))
                .quickActions(quickActions())
                .lastUpdated(displayMapper.dateTime(LocalDateTime.now()))
                .build();
    }

    private MetricCardView metric(String key, String label, long value, String helperText, String icon) {
        return MetricCardView.builder()
                .key(key)
                .label(label)
                .value(value)
                .displayValue(displayMapper.integer(value))
                .helperText(helperText)
                .icon(icon)
                .build();
    }

    private List<SeriesPointView> series(Map<String, Long> values, boolean currency) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return values.entrySet().stream()
                .map(entry -> SeriesPointView.builder()
                        .label(entry.getKey())
                        .value(entry.getValue() == null ? 0L : entry.getValue())
                        .displayValue(currency
                                ? displayMapper.currency(entry.getValue())
                                : displayMapper.integer(entry.getValue()))
                        .build())
                .collect(Collectors.toList());
    }

    private List<TopMovieView> topMovies(List<Object[]> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return rows.stream()
                .map(row -> TopMovieView.builder()
                        .title(asString(row, 0, "Unknown movie"))
                        .genre("Bookings")
                        .bookingCount(asLong(row, 1))
                        .build())
                .collect(Collectors.toList());
    }

    private List<ActivityLogView> recentLogs(List<Object[]> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return rows.stream()
                .map(row -> ActivityLogView.builder()
                        .createdAt(asDateTime(row, 0))
                        .actorLabel(asString(row, 1, "System"))
                        .action(asString(row, 2, "Activity"))
                        .description(asString(row, 4, asString(row, 3, "")))
                        .build())
                .collect(Collectors.toList());
    }

    private List<QuickActionView> quickActions() {
        return Arrays.asList(
                action("Nguoi dung", "/admin/users", "users-round"),
                action("Chi nhanh", "/admin/branches", "building"),
                action("Phim", "/admin/movies", "clapperboard"),
                action("Voucher", "/admin/vouchers", "badge-percent"));
    }

    private QuickActionView action(String label, String href, String icon) {
        return QuickActionView.builder()
                .label(label)
                .href(href)
                .icon(icon)
                .enabled(true)
                .build();
    }

    private String asString(Object[] row, int index, String fallback) {
        if (row == null || row.length <= index || row[index] == null) {
            return fallback;
        }
        return String.valueOf(row[index]);
    }

    private long asLong(Object[] row, int index) {
        if (row == null || row.length <= index || row[index] == null) {
            return 0L;
        }
        Object value = row[index];
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private LocalDateTime asDateTime(Object[] row, int index) {
        if (row == null || row.length <= index || !(row[index] instanceof LocalDateTime)) {
            return null;
        }
        return (LocalDateTime) row[index];
    }
}
