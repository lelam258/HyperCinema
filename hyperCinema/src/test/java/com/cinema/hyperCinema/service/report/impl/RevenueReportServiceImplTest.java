package com.cinema.hyperCinema.service.report.impl;

import com.cinema.hyperCinema.dto.report.revenue.RevenueReportFilter;
import com.cinema.hyperCinema.dto.report.revenue.RevenueReportView;
import com.cinema.hyperCinema.model.Branch;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.repository.BranchRepository;
import com.cinema.hyperCinema.repository.PaymentRepository;
import com.cinema.hyperCinema.repository.ShowtimeRepository;
import com.cinema.hyperCinema.service.report.RevenueReportService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RevenueReportServiceImplTest {

    @Test
    void adminReportUsesRealizedRevenueStatusAndExcludesCancelledBookings() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        BranchRepository branchRepository = mock(BranchRepository.class);
        RevenueReportServiceImpl service = service(paymentRepository, branchRepository, mockCoverageRepository());

        when(paymentRepository.summarizeCompletedRevenue(
                anyStart(), anyEnd(), isNull(), eq("Completed"), eq("Cancelled")))
                .thenReturn(new Object[] {300_000L, 220_000L, 80_000L, 2L});
        when(paymentRepository.countCompletedTickets(
                anyStart(), anyEnd(), isNull(), eq("Completed"), eq("Cancelled")))
                .thenReturn(4L);
        when(paymentRepository.findDailyCompletedRevenue(
                anyStart(), anyEnd(), isNull(), eq("Completed"), eq("Cancelled")))
                .thenReturn(List.<Object[]>of(new Object[] {java.sql.Date.valueOf("2026-07-14"), 300_000L, 2L}));
        when(paymentRepository.findBranchRevenueRanking(
                anyStart(), anyEnd(), isNull(), eq("Completed"), eq("Cancelled")))
                .thenReturn(List.of());

        RevenueReportView report = service.getAdminReport(RevenueReportFilter.builder()
                .dateFrom(LocalDate.of(2026, 7, 1))
                .dateTo(LocalDate.of(2026, 7, 14))
                .build());

        assertThat(report.getTotalRevenue()).isEqualTo(300_000L);
        assertThat(report.getTicketRevenue()).isEqualTo(220_000L);
        assertThat(report.getFoodRevenue()).isEqualTo(80_000L);
        assertThat(report.getPaidBookingCount()).isEqualTo(2L);
        assertThat(report.getTicketCount()).isEqualTo(4L);
        assertThat(report.getTrend()).hasSize(1);

        verify(paymentRepository).summarizeCompletedRevenue(
                anyStart(), anyEnd(), isNull(),
                eq(RevenueReportService.REALIZED_PAYMENT_STATUS),
                eq(RevenueReportService.EXCLUDED_BOOKING_STATUS));
    }

    @Test
    void adminRankingOrdersRowsAndCalculatesContributionPercent() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        BranchRepository branchRepository = mock(BranchRepository.class);
        RevenueReportServiceImpl service = service(paymentRepository, branchRepository, mockCoverageRepository());

        when(paymentRepository.summarizeCompletedRevenue(
                anyStart(), anyEnd(), isNull(), eq("Completed"), eq("Cancelled")))
                .thenReturn(new Object[] {1_000_000L, 700_000L, 300_000L, 10L});
        when(paymentRepository.findDailyCompletedRevenue(
                anyStart(), anyEnd(), isNull(), eq("Completed"), eq("Cancelled")))
                .thenReturn(List.of());
        when(paymentRepository.countCompletedTickets(
                anyStart(), anyEnd(), isNull(), eq("Completed"), eq("Cancelled")))
                .thenReturn(20L);
        when(paymentRepository.findBranchRevenueRanking(
                anyStart(), anyEnd(), isNull(), eq("Completed"), eq("Cancelled")))
                .thenReturn(List.of(
                        new Object[] {1, "Branch A", 600_000L, 450_000L, 150_000L, 6L},
                        new Object[] {2, "Branch B", 400_000L, 250_000L, 150_000L, 4L}
                ));
        when(paymentRepository.countCompletedTickets(
                anyStart(), anyEnd(), eq(1), eq("Completed"), eq("Cancelled")))
                .thenReturn(12L);
        when(paymentRepository.countCompletedTickets(
                anyStart(), anyEnd(), eq(2), eq("Completed"), eq("Cancelled")))
                .thenReturn(8L);

        RevenueReportView report = service.getAdminReport(new RevenueReportFilter());

        assertThat(report.getBranchRanking()).hasSize(2);
        assertThat(report.getBranchRanking().get(0).getRank()).isEqualTo(1);
        assertThat(report.getBranchRanking().get(0).getBranchName()).isEqualTo("Branch A");
        assertThat(report.getBranchRanking().get(0).getContributionPercent()).isEqualTo(60D);
        assertThat(report.getBranchRanking().get(1).getContributionPercent()).isEqualTo(40D);
    }

    @Test
    void managerReportAlwaysUsesAssignedBranchScope() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        BranchRepository branchRepository = mock(BranchRepository.class);
        RevenueReportServiceImpl service = service(paymentRepository, branchRepository, mockCoverageRepository());
        Branch branch = branch(7, "HyperCinema Q1");
        User manager = new User();
        manager.setBranch(branch);

        when(paymentRepository.summarizeCompletedRevenue(
                anyStart(), anyEnd(), eq(7), eq("Completed"), eq("Cancelled")))
                .thenReturn(new Object[] {500_000L, 400_000L, 100_000L, 5L});
        when(paymentRepository.countCompletedTickets(
                anyStart(), anyEnd(), eq(7), eq("Completed"), eq("Cancelled")))
                .thenReturn(9L);
        when(paymentRepository.findDailyCompletedRevenue(
                anyStart(), anyEnd(), eq(7), eq("Completed"), eq("Cancelled")))
                .thenReturn(List.of());

        RevenueReportView report = service.getManagerReport(manager, RevenueReportFilter.builder()
                .branchId(999)
                .dateFrom(LocalDate.of(2026, 7, 1))
                .dateTo(LocalDate.of(2026, 7, 14))
                .build());

        assertThat(report.getBranchId()).isEqualTo(7);
        assertThat(report.getBranchName()).isEqualTo("HyperCinema Q1");
        assertThat(report.getTotalRevenue()).isEqualTo(500_000L);
        verify(paymentRepository).summarizeCompletedRevenue(
                anyStart(), anyEnd(), eq(7), eq("Completed"), eq("Cancelled"));
    }

    @Test
    void managerWithoutBranchGetsNoScopeReport() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        BranchRepository branchRepository = mock(BranchRepository.class);
        RevenueReportServiceImpl service = service(paymentRepository, branchRepository, mockCoverageRepository());

        RevenueReportView report = service.getManagerReport(new User(), new RevenueReportFilter());

        assertThat(report.isNoScope()).isTrue();
        assertThat(report.getTotalRevenue()).isZero();
        assertThat(report.getMessage()).contains("chua duoc gan chi nhanh");
    }

    @Test
    void adminCanFilterOneBranch() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        BranchRepository branchRepository = mock(BranchRepository.class);
        RevenueReportServiceImpl service = service(paymentRepository, branchRepository, mockCoverageRepository());
        when(branchRepository.findById(3)).thenReturn(Optional.of(branch(3, "HyperCinema Thu Duc")));
        when(paymentRepository.summarizeCompletedRevenue(
                anyStart(), anyEnd(), eq(3), eq("Completed"), eq("Cancelled")))
                .thenReturn(new Object[] {100_000L, 100_000L, 0L, 1L});
        when(paymentRepository.countCompletedTickets(
                anyStart(), anyEnd(), eq(3), eq("Completed"), eq("Cancelled")))
                .thenReturn(2L);
        when(paymentRepository.findDailyCompletedRevenue(
                anyStart(), anyEnd(), eq(3), eq("Completed"), eq("Cancelled")))
                .thenReturn(List.of());
        when(paymentRepository.findBranchRevenueRanking(
                anyStart(), anyEnd(), eq(3), eq("Completed"), eq("Cancelled")))
                .thenReturn(List.<Object[]>of(new Object[] {3, "HyperCinema Thu Duc", 100_000L, 100_000L, 0L, 1L}));

        RevenueReportView report = service.getAdminReport(RevenueReportFilter.builder().branchId(3).build());

        assertThat(report.getBranchId()).isEqualTo(3);
        assertThat(report.getBranchName()).isEqualTo("HyperCinema Thu Duc");
        assertThat(report.getBranchRanking()).hasSize(1);
    }

    @Test
    void presetWeekIgnoresSubmittedCustomDates() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        BranchRepository branchRepository = mock(BranchRepository.class);
        RevenueReportServiceImpl service = service(paymentRepository, branchRepository, mockCoverageRepository());
        when(paymentRepository.summarizeCompletedRevenue(
                anyStart(), anyEnd(), isNull(), eq("Completed"), eq("Cancelled")))
                .thenReturn(new Object[] {0L, 0L, 0L, 0L});
        when(paymentRepository.findDailyCompletedRevenue(
                anyStart(), anyEnd(), isNull(), eq("Completed"), eq("Cancelled")))
                .thenReturn(List.of());
        when(paymentRepository.findBranchRevenueRanking(
                anyStart(), anyEnd(), isNull(), eq("Completed"), eq("Cancelled")))
                .thenReturn(List.of());

        RevenueReportView report = service.getAdminReport(RevenueReportFilter.builder()
                .preset("week")
                .dateFrom(LocalDate.of(2026, 1, 28))
                .dateTo(LocalDate.of(2026, 7, 14))
                .build());

        assertThat(report.getPreset()).isEqualTo("week");
        assertThat(report.getDateFrom()).isEqualTo(LocalDate.now().minusDays(6));
        assertThat(report.getDateTo()).isEqualTo(LocalDate.now());
    }

    @Test
    void showtimeCoverageCalculatesPotentialRevenueCoverageAndGap() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        BranchRepository branchRepository = mock(BranchRepository.class);
        ShowtimeRepository showtimeRepository = mockCoverageRepository();
        RevenueReportServiceImpl service = service(paymentRepository, branchRepository, showtimeRepository);
        when(paymentRepository.summarizeCompletedRevenue(
                anyStart(), anyEnd(), isNull(), eq("Completed"), eq("Cancelled")))
                .thenReturn(new Object[] {0L, 0L, 0L, 0L});
        when(paymentRepository.findDailyCompletedRevenue(
                anyStart(), anyEnd(), isNull(), eq("Completed"), eq("Cancelled")))
                .thenReturn(List.of());
        when(paymentRepository.findBranchRevenueRanking(
                anyStart(), anyEnd(), isNull(), eq("Completed"), eq("Cancelled")))
                .thenReturn(List.of());
        when(showtimeRepository.findShowtimeCoverageRows(
                anyStart(), anyEnd(), isNull(), eq("Completed"), eq("Cancelled")))
                .thenReturn(List.<Object[]>of(new Object[] {
                        99, "Movie A", 3, "Branch A", "Hall 1",
                        LocalDateTime.of(2026, 7, 14, 18, 0),
                        100, 100_000, 80, 8_000_000
                }));

        RevenueReportView report = service.getAdminReport(new RevenueReportFilter());

        assertThat(report.getShowtimeCoverage()).hasSize(1);
        var row = report.getShowtimeCoverage().get(0);
        assertThat(row.getPotentialTicketRevenue()).isEqualTo(10_000_000L);
        assertThat(row.getRevenueGap()).isEqualTo(2_000_000L);
        assertThat(row.getSeatCoveragePercent()).isEqualTo(80D);
        assertThat(row.getRevenueCoveragePercent()).isEqualTo(80D);
        assertThat(row.getEvaluationLabel()).isEqualTo("Tot");
    }

    @Test
    void showtimeCoverageEvaluatesAverageLowAndNoSalesRows() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        BranchRepository branchRepository = mock(BranchRepository.class);
        ShowtimeRepository showtimeRepository = mockCoverageRepository();
        RevenueReportServiceImpl service = service(paymentRepository, branchRepository, showtimeRepository);
        when(paymentRepository.summarizeCompletedRevenue(
                anyStart(), anyEnd(), isNull(), eq("Completed"), eq("Cancelled")))
                .thenReturn(new Object[] {0L, 0L, 0L, 0L});
        when(paymentRepository.findDailyCompletedRevenue(
                anyStart(), anyEnd(), isNull(), eq("Completed"), eq("Cancelled")))
                .thenReturn(List.of());
        when(paymentRepository.findBranchRevenueRanking(
                anyStart(), anyEnd(), isNull(), eq("Completed"), eq("Cancelled")))
                .thenReturn(List.of());
        when(showtimeRepository.findShowtimeCoverageRows(
                anyStart(), anyEnd(), isNull(), eq("Completed"), eq("Cancelled")))
                .thenReturn(List.of(
                        coverageRow(1, 1, "Branch A", 100, 100_000, 60, 6_000_000),
                        coverageRow(2, 1, "Branch A", 100, 100_000, 10, 1_000_000),
                        coverageRow(3, 1, "Branch A", 100, 100_000, 0, 0)
                ));

        RevenueReportView report = service.getAdminReport(new RevenueReportFilter());

        assertThat(report.getShowtimeCoverage())
                .extracting("evaluationLabel")
                .containsExactly("Trung binh", "Thap", "Chua co doanh thu");
        assertThat(report.getCoverageSummary().getLowCoverageShowtimeCount()).isEqualTo(2L);
    }

    @Test
    void branchCoverageSummarizesRowsByBranch() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        BranchRepository branchRepository = mock(BranchRepository.class);
        ShowtimeRepository showtimeRepository = mockCoverageRepository();
        RevenueReportServiceImpl service = service(paymentRepository, branchRepository, showtimeRepository);
        when(paymentRepository.summarizeCompletedRevenue(
                anyStart(), anyEnd(), isNull(), eq("Completed"), eq("Cancelled")))
                .thenReturn(new Object[] {0L, 0L, 0L, 0L});
        when(paymentRepository.findDailyCompletedRevenue(
                anyStart(), anyEnd(), isNull(), eq("Completed"), eq("Cancelled")))
                .thenReturn(List.of());
        when(paymentRepository.findBranchRevenueRanking(
                anyStart(), anyEnd(), isNull(), eq("Completed"), eq("Cancelled")))
                .thenReturn(List.of());
        when(showtimeRepository.findShowtimeCoverageRows(
                anyStart(), anyEnd(), isNull(), eq("Completed"), eq("Cancelled")))
                .thenReturn(List.of(
                        coverageRow(1, 1, "Branch A", 100, 100_000, 50, 5_000_000),
                        coverageRow(2, 1, "Branch A", 100, 100_000, 25, 2_500_000),
                        coverageRow(3, 2, "Branch B", 100, 100_000, 80, 8_000_000)
                ));

        RevenueReportView report = service.getAdminReport(new RevenueReportFilter());

        assertThat(report.getBranchCoverage()).hasSize(2);
        assertThat(report.getBranchCoverage().get(0).getBranchName()).isEqualTo("Branch B");
        assertThat(report.getBranchCoverage().get(0).getAverageRevenueCoveragePercent()).isEqualTo(80D);
        assertThat(report.getBranchCoverage().get(1).getBranchName()).isEqualTo("Branch A");
        assertThat(report.getBranchCoverage().get(1).getRevenueGap()).isEqualTo(12_500_000L);
    }

    private static RevenueReportServiceImpl service(PaymentRepository paymentRepository,
                                                    BranchRepository branchRepository,
                                                    ShowtimeRepository showtimeRepository) {
        return new RevenueReportServiceImpl(paymentRepository, branchRepository, showtimeRepository);
    }

    private static ShowtimeRepository mockCoverageRepository() {
        ShowtimeRepository showtimeRepository = mock(ShowtimeRepository.class);
        when(showtimeRepository.findShowtimeCoverageRows(
                anyStart(), anyEnd(), nullable(Integer.class), eq("Completed"), eq("Cancelled")))
                .thenReturn(List.of());
        return showtimeRepository;
    }

    private static Object[] coverageRow(int showtimeId,
                                        int branchId,
                                        String branchName,
                                        int capacity,
                                        int price,
                                        int paidTickets,
                                        long actualRevenue) {
        return new Object[] {
                showtimeId, "Movie " + showtimeId, branchId, branchName, "Hall",
                LocalDateTime.of(2026, 7, 14, 18, 0),
                capacity, price, paidTickets, actualRevenue
        };
    }

    private static LocalDateTime anyStart() {
        return org.mockito.ArgumentMatchers.any(LocalDateTime.class);
    }

    private static LocalDateTime anyEnd() {
        return org.mockito.ArgumentMatchers.any(LocalDateTime.class);
    }

    private static Branch branch(Integer id, String name) {
        Branch branch = new Branch();
        branch.setBranchId(id);
        branch.setName(name);
        return branch;
    }
}
