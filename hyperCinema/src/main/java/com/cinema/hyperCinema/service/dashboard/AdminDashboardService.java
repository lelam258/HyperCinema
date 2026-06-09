package com.cinema.hyperCinema.service.dashboard;

import java.util.List;
import java.util.Map;

/**
 * Service cung cấp dữ liệu tổng hợp cho Admin Dashboard.
 */
public interface AdminDashboardService {

    // ── KPI Cards ──
    long countTotalUsers();
    long countActiveUsers();
    long countTotalBranches();
    long countActiveBranches();
    long countTotalMovies();
    long countNowShowingMovies();
    long countTodayBookings();
    long sumTodayRevenue();

    // ── Charts ──
    /** Doanh thu 7 ngày gần nhất: key=ngày (yyyy-MM-dd), value=tổng tiền */
    Map<String, Long> getRevenueLastDays(int days);

    /** Phân bố user theo role: key=roleName, value=count */
    Map<String, Long> getUserDistributionByRole();

    // ── Tables ──
    /** Top N phim bán chạy nhất (theo số booking) */
    List<Object[]> getTopMoviesByBookingCount(int limit);

    /** N audit log gần nhất */
    List<Object[]> getRecentAuditLogs(int limit);
}
