package com.cinema.hyperCinema.service.dashboard;

import java.util.List;
import java.util.Map;

public interface BranchManagerDashboardService {

    // ── KPI Cards ──
    long sumBranchRevenueThisMonth(Integer branchId);
    long countBranchTicketsThisMonth(Integer branchId);
    long countBranchBookingsToday(Integer branchId);

    // ── Charts ──
    Map<String, Long> getBranchRevenueLastDays(Integer branchId, int days);

    // ── Tables ──
    List<Object[]> getBranchTopMovies(Integer branchId, int limit);
}
