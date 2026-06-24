package com.cinema.hyperCinema.service.dashboard.impl;

import com.cinema.hyperCinema.repository.*;
import com.cinema.hyperCinema.service.dashboard.AdminDashboardService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final MovieRepository movieRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final RoleRepository roleRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminDashboardServiceImpl(
            UserRepository userRepository,
            BranchRepository branchRepository,
            MovieRepository movieRepository,
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            RoleRepository roleRepository,
            AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
        this.movieRepository = movieRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.roleRepository = roleRepository;
        this.auditLogRepository = auditLogRepository;
    }

    // ── KPI Cards ──

    @Override
    public long countTotalUsers() {
        return userRepository.count();
    }

    @Override
    public long countActiveUsers() {
        return userRepository.countByStatus("Active");
    }

    @Override
    public long countTotalBranches() {
        return branchRepository.count();
    }

    @Override
    public long countActiveBranches() {
        return branchRepository.countByStatus("Active");
    }

    @Override
    public long countTotalMovies() {
        return movieRepository.count();
    }

    @Override
    public long countNowShowingMovies() {
        return movieRepository.countByStatus("NowShowing");
    }

    @Override
    public long countTodayBookings() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        return bookingRepository.countByCreatedAtBetween(startOfDay, endOfDay);
    }

    @Override
    public long sumTodayRevenue() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        Long revenue = paymentRepository.sumAmountByCreatedAtBetweenAndStatus(startOfDay, endOfDay, "Completed");
        return revenue != null ? revenue : 0L;
    }

    // ── Charts ──

    @Override
    public Map<String, Long> getRevenueLastDays(int days) {
        Map<String, Long> result = new LinkedHashMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
        LocalDate today = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = start.plusDays(1);
            Long revenue = paymentRepository.sumAmountByCreatedAtBetweenAndStatus(start, end, "Completed");
            result.put(date.format(fmt), revenue != null ? revenue : 0L);
        }
        return result;
    }

    @Override
    public Map<String, Long> getUserDistributionByRole() {
        Map<String, Long> result = new LinkedHashMap<>();
        List<Object[]> rows = userRepository.countUsersByRole();
        for (Object[] row : rows) {
            result.put((String) row[0], (Long) row[1]);
        }
        return result;
    }

    // ── Tables ──

    @Override
    public List<Object[]> getTopMoviesByBookingCount(int limit) {
        return bookingRepository.findTopMoviesByBookingCount(limit);
    }

    @Override
    public List<Object[]> getRecentAuditLogs(int limit) {
        return auditLogRepository.findRecentLogs(limit);
    }
}
