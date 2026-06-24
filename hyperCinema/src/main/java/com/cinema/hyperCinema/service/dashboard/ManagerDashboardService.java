package com.cinema.hyperCinema.service.dashboard;

import java.util.List;
import java.util.Map;

public interface ManagerDashboardService {

    // ── KPI Cards ──
    long sumChainRevenueThisMonth();
    long countChainTicketsThisMonth();
    long countActiveBranches();
    long countNowShowingMovies();

    // ── Charts ──
    Map<String, Long> getRevenueLastDays(int days);

    // ── Tables ──
    List<Object[]> getBranchLeaderboardThisMonth();
    List<Object[]> getTopMoviesThisMonth(int limit);
}
