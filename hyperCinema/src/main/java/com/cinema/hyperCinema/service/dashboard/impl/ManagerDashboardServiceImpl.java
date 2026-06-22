package com.cinema.hyperCinema.service.dashboard.impl;

import com.cinema.hyperCinema.repository.BookingRepository;
import com.cinema.hyperCinema.repository.BranchRepository;
import com.cinema.hyperCinema.repository.MovieRepository;
import com.cinema.hyperCinema.repository.PaymentRepository;
import com.cinema.hyperCinema.service.dashboard.ManagerDashboardService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ManagerDashboardServiceImpl implements ManagerDashboardService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final BranchRepository branchRepository;
    private final MovieRepository movieRepository;

    public ManagerDashboardServiceImpl(PaymentRepository paymentRepository,
                                       BookingRepository bookingRepository,
                                       BranchRepository branchRepository,
                                       MovieRepository movieRepository) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.branchRepository = branchRepository;
        this.movieRepository = movieRepository;
    }

    @Override
    public long sumChainRevenueThisMonth() {
        LocalDateTime startOfMonth = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
        LocalDateTime endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()).plusDays(1).atStartOfDay();
        Long sum = paymentRepository.sumAmountByCreatedAtBetweenAndStatus(startOfMonth, endOfMonth, "Completed");
        return sum != null ? sum : 0L;
    }

    @Override
    public long countChainTicketsThisMonth() {
        LocalDateTime startOfMonth = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
        LocalDateTime endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()).plusDays(1).atStartOfDay();
        return bookingRepository.countTicketsByBookingStatusAndCreatedAtBetween(startOfMonth, endOfMonth);
    }

    @Override
    public long countActiveBranches() {
        return branchRepository.countByStatus("Active");
    }

    @Override
    public long countNowShowingMovies() {
        return movieRepository.countByStatus("NowShowing");
    }

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
    public List<Object[]> getBranchLeaderboardThisMonth() {
        LocalDateTime startOfMonth = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
        LocalDateTime endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()).plusDays(1).atStartOfDay();
        return paymentRepository.findBranchRevenueLeaderboard(startOfMonth, endOfMonth);
    }

    @Override
    public List<Object[]> getTopMoviesThisMonth(int limit) {
        // Có thể reuse method findTopMoviesByBookingCount (toàn thời gian) 
        // hoặc viết thêm 1 query có filter by date.
        // Tạm thời dùng method có sẵn.
        return bookingRepository.findTopMoviesByBookingCount(limit);
    }
}
