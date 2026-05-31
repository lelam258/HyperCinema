package com.cinema.hyperCinema.service.dashboard.impl;

import com.cinema.hyperCinema.repository.BookingRepository;
import com.cinema.hyperCinema.repository.PaymentRepository;
import com.cinema.hyperCinema.service.dashboard.BranchManagerDashboardService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class BranchManagerDashboardServiceImpl implements BranchManagerDashboardService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    public BranchManagerDashboardServiceImpl(PaymentRepository paymentRepository,
                                             BookingRepository bookingRepository) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    public long sumBranchRevenueThisMonth(Integer branchId) {
        LocalDateTime startOfMonth = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
        LocalDateTime endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()).plusDays(1).atStartOfDay();
        Long sum = paymentRepository.sumRevenueByBranchAndDateRange(branchId, startOfMonth, endOfMonth);
        return sum != null ? sum : 0L;
    }

    @Override
    public long countBranchTicketsThisMonth(Integer branchId) {
        LocalDateTime startOfMonth = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
        LocalDateTime endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()).plusDays(1).atStartOfDay();
        return bookingRepository.countTicketsByBranchIdAndDateRange(branchId, startOfMonth, endOfMonth);
    }

    @Override
    public long countBranchBookingsToday(Integer branchId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        return bookingRepository.countBookingsByBranchIdAndDateRange(branchId, startOfDay, endOfDay);
    }

    @Override
    public Map<String, Long> getBranchRevenueLastDays(Integer branchId, int days) {
        Map<String, Long> result = new LinkedHashMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
        LocalDate today = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = start.plusDays(1);
            Long revenue = paymentRepository.sumRevenueByBranchAndDateRange(branchId, start, end);
            result.put(date.format(fmt), revenue != null ? revenue : 0L);
        }
        return result;
    }

    @Override
    public List<Object[]> getBranchTopMovies(Integer branchId, int limit) {
        return bookingRepository.findTopMoviesByBranchId(branchId, limit);
    }
}
