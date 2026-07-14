package com.cinema.hyperCinema.service.report.impl;

import com.cinema.hyperCinema.dto.report.revenue.BranchCoverageSummary;
import com.cinema.hyperCinema.dto.report.revenue.BranchRevenueRankingRow;
import com.cinema.hyperCinema.dto.report.revenue.RevenueReportFilter;
import com.cinema.hyperCinema.dto.report.revenue.RevenueReportView;
import com.cinema.hyperCinema.dto.report.revenue.RevenueTrendPoint;
import com.cinema.hyperCinema.dto.report.revenue.ShowtimeCoverageEvaluation;
import com.cinema.hyperCinema.dto.report.revenue.ShowtimeCoverageRow;
import com.cinema.hyperCinema.model.Branch;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.repository.BranchRepository;
import com.cinema.hyperCinema.repository.PaymentRepository;
import com.cinema.hyperCinema.repository.ShowtimeRepository;
import com.cinema.hyperCinema.service.report.RevenueReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class RevenueReportServiceImpl implements RevenueReportService {

    private static final DateTimeFormatter LAST_UPDATED_FORMAT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final DateTimeFormatter TREND_LABEL_FORMAT = DateTimeFormatter.ofPattern("dd/MM");

    private final PaymentRepository paymentRepository;
    private final BranchRepository branchRepository;
    private final ShowtimeRepository showtimeRepository;

    public RevenueReportServiceImpl(PaymentRepository paymentRepository,
                                    BranchRepository branchRepository,
                                    ShowtimeRepository showtimeRepository) {
        this.paymentRepository = paymentRepository;
        this.branchRepository = branchRepository;
        this.showtimeRepository = showtimeRepository;
    }

    @Override
    public RevenueReportView getAdminReport(RevenueReportFilter filter) {
        NormalizedFilter normalized = normalize(filter);
        Integer branchId = normalized.filter().getBranchId();
        String branchName = branchId == null
                ? "Tat ca chi nhanh"
                : branchRepository.findById(branchId).map(Branch::getName).orElse("Chi nhanh khong ton tai");

        RevenueReportView report = buildReport("ADMIN", branchId, branchName, normalized);
        report.setBranchRanking(buildRanking(normalized.start(), normalized.end(), branchId));
        addCoverage(report, normalized.start(), normalized.end(), branchId);
        return report;
    }

    @Override
    public RevenueReportView getManagerReport(User manager, RevenueReportFilter filter) {
        NormalizedFilter normalized = normalize(filter);
        Branch branch = manager != null ? manager.getBranch() : null;
        if (branch == null || branch.getBranchId() == null) {
            return emptyNoScopeReport("MANAGER", normalized, "Tai khoan quan ly chua duoc gan chi nhanh.");
        }

        RevenueReportFilter scopedFilter = RevenueReportFilter.builder()
                .dateFrom(normalized.filter().getDateFrom())
                .dateTo(normalized.filter().getDateTo())
                .preset(normalized.filter().getPreset())
                .branchId(branch.getBranchId())
                .build();
        NormalizedFilter scoped = new NormalizedFilter(scopedFilter, normalized.start(), normalized.end(), normalized.message());
        RevenueReportView report = buildReport("MANAGER", branch.getBranchId(), branch.getName(), scoped);
        report.setBranchRanking(Collections.emptyList());
        addCoverage(report, scoped.start(), scoped.end(), branch.getBranchId());
        return report;
    }

    private RevenueReportView buildReport(String scopeRole,
                                          Integer branchId,
                                          String branchName,
                                          NormalizedFilter normalized) {
        Object[] summary = firstRow(paymentRepository.summarizeCompletedRevenue(
                normalized.start(),
                normalized.end(),
                branchId,
                REALIZED_PAYMENT_STATUS,
                EXCLUDED_BOOKING_STATUS));
        long totalRevenue = numberAt(summary, 0);
        long ticketRevenue = numberAt(summary, 1);
        long foodRevenue = numberAt(summary, 2);
        long paidBookingCount = numberAt(summary, 3);
        long ticketCount = paymentRepository.countCompletedTickets(
                normalized.start(),
                normalized.end(),
                branchId,
                REALIZED_PAYMENT_STATUS,
                EXCLUDED_BOOKING_STATUS);
        long averageOrderValue = paidBookingCount == 0 ? 0 : totalRevenue / paidBookingCount;

        return RevenueReportView.builder()
                .scopeRole(scopeRole)
                .branchId(branchId)
                .branchName(branchName)
                .dateFrom(normalized.filter().getDateFrom())
                .dateTo(normalized.filter().getDateTo())
                .preset(normalized.filter().getPreset())
                .totalRevenue(totalRevenue)
                .ticketRevenue(ticketRevenue)
                .foodRevenue(foodRevenue)
                .paidBookingCount(paidBookingCount)
                .ticketCount(ticketCount)
                .averageOrderValue(averageOrderValue)
                .trend(buildTrend(normalized.start(), normalized.end(), branchId))
                .branchRanking(Collections.emptyList())
                .coverageSummary(emptyCoverageSummary(branchId, branchName))
                .branchCoverage(Collections.emptyList())
                .showtimeCoverage(Collections.emptyList())
                .lastUpdated(LocalDateTime.now().format(LAST_UPDATED_FORMAT))
                .noScope(false)
                .message(normalized.message())
                .build();
    }

    private RevenueReportView emptyNoScopeReport(String scopeRole, NormalizedFilter normalized, String message) {
        return RevenueReportView.builder()
                .scopeRole(scopeRole)
                .dateFrom(normalized.filter().getDateFrom())
                .dateTo(normalized.filter().getDateTo())
                .preset(normalized.filter().getPreset())
                .totalRevenue(0)
                .ticketRevenue(0)
                .foodRevenue(0)
                .paidBookingCount(0)
                .ticketCount(0)
                .averageOrderValue(0)
                .trend(Collections.emptyList())
                .branchRanking(Collections.emptyList())
                .coverageSummary(null)
                .branchCoverage(Collections.emptyList())
                .showtimeCoverage(Collections.emptyList())
                .lastUpdated(LocalDateTime.now().format(LAST_UPDATED_FORMAT))
                .noScope(true)
                .message(message)
                .build();
    }

    private void addCoverage(RevenueReportView report,
                             LocalDateTime start,
                             LocalDateTime end,
                             Integer branchId) {
        List<ShowtimeCoverageRow> rows = buildShowtimeCoverage(start, end, branchId);
        report.setShowtimeCoverage(rows);
        report.setCoverageSummary(summarizeCoverage(report.getBranchId(), report.getBranchName(), rows));
        report.setBranchCoverage(buildBranchCoverage(rows));
    }

    private List<ShowtimeCoverageRow> buildShowtimeCoverage(LocalDateTime start,
                                                            LocalDateTime end,
                                                            Integer branchId) {
        List<Object[]> rows = showtimeRepository.findShowtimeCoverageRows(
                start,
                end,
                branchId,
                REALIZED_PAYMENT_STATUS,
                EXCLUDED_BOOKING_STATUS);
        List<ShowtimeCoverageRow> coverageRows = new ArrayList<>();
        for (Object[] row : rows) {
            long capacity = numberAt(row, 6);
            long showtimePrice = numberAt(row, 7);
            long paidTicketCount = numberAt(row, 8);
            long actualTicketRevenue = numberAt(row, 9);
            long potentialTicketRevenue = Math.max(0L, capacity * showtimePrice);
            double seatCoverage = capacity == 0 ? 0D : paidTicketCount * 100D / capacity;
            double revenueCoverage = potentialTicketRevenue == 0 ? 0D : actualTicketRevenue * 100D / potentialTicketRevenue;
            ShowtimeCoverageEvaluation evaluation = evaluateCoverage(paidTicketCount, potentialTicketRevenue, revenueCoverage);

            coverageRows.add(ShowtimeCoverageRow.builder()
                    .showtimeId(intAt(row, 0))
                    .movieTitle(stringAt(row, 1))
                    .branchId(intAt(row, 2))
                    .branchName(stringAt(row, 3))
                    .hallName(stringAt(row, 4))
                    .startTime(toLocalDateTime(row[5]))
                    .capacity(capacity)
                    .paidTicketCount(paidTicketCount)
                    .seatCoveragePercent(seatCoverage)
                    .actualTicketRevenue(actualTicketRevenue)
                    .potentialTicketRevenue(potentialTicketRevenue)
                    .revenueCoveragePercent(revenueCoverage)
                    .revenueGap(Math.max(0L, potentialTicketRevenue - actualTicketRevenue))
                    .evaluation(evaluation)
                    .evaluationLabel(evaluation.getDisplayName())
                    .build());
        }
        return coverageRows;
    }

    private BranchCoverageSummary summarizeCoverage(Integer branchId,
                                                    String branchName,
                                                    List<ShowtimeCoverageRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return emptyCoverageSummary(branchId, branchName);
        }
        long showtimeCount = rows.size();
        long totalCapacity = rows.stream().mapToLong(ShowtimeCoverageRow::getCapacity).sum();
        long paidTicketCount = rows.stream().mapToLong(ShowtimeCoverageRow::getPaidTicketCount).sum();
        long actualTicketRevenue = rows.stream().mapToLong(ShowtimeCoverageRow::getActualTicketRevenue).sum();
        long potentialTicketRevenue = rows.stream().mapToLong(ShowtimeCoverageRow::getPotentialTicketRevenue).sum();
        long lowCoverageCount = rows.stream()
                .filter(row -> row.getEvaluation() == ShowtimeCoverageEvaluation.LOW
                        || row.getEvaluation() == ShowtimeCoverageEvaluation.NO_SALES)
                .count();
        return BranchCoverageSummary.builder()
                .branchId(branchId)
                .branchName(branchName)
                .showtimeCount(showtimeCount)
                .totalCapacity(totalCapacity)
                .paidTicketCount(paidTicketCount)
                .averageSeatCoveragePercent(totalCapacity == 0 ? 0D : paidTicketCount * 100D / totalCapacity)
                .actualTicketRevenue(actualTicketRevenue)
                .potentialTicketRevenue(potentialTicketRevenue)
                .averageRevenueCoveragePercent(potentialTicketRevenue == 0 ? 0D : actualTicketRevenue * 100D / potentialTicketRevenue)
                .revenueGap(Math.max(0L, potentialTicketRevenue - actualTicketRevenue))
                .lowCoverageShowtimeCount(lowCoverageCount)
                .build();
    }

    private List<BranchCoverageSummary> buildBranchCoverage(List<ShowtimeCoverageRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Integer, List<ShowtimeCoverageRow>> grouped = new LinkedHashMap<>();
        for (ShowtimeCoverageRow row : rows) {
            grouped.computeIfAbsent(row.getBranchId(), ignored -> new ArrayList<>()).add(row);
        }
        List<BranchCoverageSummary> summaries = new ArrayList<>();
        for (List<ShowtimeCoverageRow> branchRows : grouped.values()) {
            ShowtimeCoverageRow first = branchRows.get(0);
            summaries.add(summarizeCoverage(first.getBranchId(), first.getBranchName(), branchRows));
        }
        summaries.sort((left, right) -> Long.compare(right.getActualTicketRevenue(), left.getActualTicketRevenue()));
        return summaries;
    }

    private BranchCoverageSummary emptyCoverageSummary(Integer branchId, String branchName) {
        return BranchCoverageSummary.builder()
                .branchId(branchId)
                .branchName(branchName)
                .showtimeCount(0)
                .totalCapacity(0)
                .paidTicketCount(0)
                .averageSeatCoveragePercent(0D)
                .actualTicketRevenue(0)
                .potentialTicketRevenue(0)
                .averageRevenueCoveragePercent(0D)
                .revenueGap(0)
                .lowCoverageShowtimeCount(0)
                .build();
    }

    private ShowtimeCoverageEvaluation evaluateCoverage(long paidTicketCount,
                                                        long potentialTicketRevenue,
                                                        double revenueCoveragePercent) {
        if (potentialTicketRevenue <= 0) {
            return ShowtimeCoverageEvaluation.NO_DATA;
        }
        if (paidTicketCount <= 0) {
            return ShowtimeCoverageEvaluation.NO_SALES;
        }
        if (revenueCoveragePercent >= 80D) {
            return ShowtimeCoverageEvaluation.GOOD;
        }
        if (revenueCoveragePercent >= 50D) {
            return ShowtimeCoverageEvaluation.AVERAGE;
        }
        return ShowtimeCoverageEvaluation.LOW;
    }

    private List<RevenueTrendPoint> buildTrend(LocalDateTime start, LocalDateTime end, Integer branchId) {
        List<Object[]> rows = paymentRepository.findDailyCompletedRevenue(
                start,
                end,
                branchId,
                REALIZED_PAYMENT_STATUS,
                EXCLUDED_BOOKING_STATUS);
        List<RevenueTrendPoint> trend = new ArrayList<>();
        for (Object[] row : rows) {
            LocalDate date = toLocalDate(row[0]);
            trend.add(RevenueTrendPoint.builder()
                    .date(date)
                    .label(date.format(TREND_LABEL_FORMAT))
                    .revenue(numberAt(row, 1))
                    .paidBookingCount(numberAt(row, 2))
                    .build());
        }
        return trend;
    }

    private List<BranchRevenueRankingRow> buildRanking(LocalDateTime start, LocalDateTime end, Integer branchId) {
        List<Object[]> rows = paymentRepository.findBranchRevenueRanking(
                start,
                end,
                branchId,
                REALIZED_PAYMENT_STATUS,
                EXCLUDED_BOOKING_STATUS);
        long chainTotal = rows.stream().mapToLong(row -> numberAt(row, 2)).sum();
        List<BranchRevenueRankingRow> ranking = new ArrayList<>();
        int rank = 1;
        for (Object[] row : rows) {
            Integer rowBranchId = ((Number) row[0]).intValue();
            long totalRevenue = numberAt(row, 2);
            long ticketCount = paymentRepository.countCompletedTickets(
                    start,
                    end,
                    rowBranchId,
                    REALIZED_PAYMENT_STATUS,
                    EXCLUDED_BOOKING_STATUS);
            ranking.add(BranchRevenueRankingRow.builder()
                    .rank(rank++)
                    .branchId(rowBranchId)
                    .branchName((String) row[1])
                    .totalRevenue(totalRevenue)
                    .ticketRevenue(numberAt(row, 3))
                    .foodRevenue(numberAt(row, 4))
                    .paidBookingCount(numberAt(row, 5))
                    .ticketCount(ticketCount)
                    .contributionPercent(chainTotal == 0 ? 0D : totalRevenue * 100D / chainTotal)
                    .build());
        }
        return ranking;
    }

    private NormalizedFilter normalize(RevenueReportFilter source) {
        RevenueReportFilter filter = source != null ? source : new RevenueReportFilter();
        LocalDate today = LocalDate.now();
        String preset = filter.getPreset() == null || filter.getPreset().isBlank()
                ? "month"
                : filter.getPreset().trim().toLowerCase(Locale.ROOT);

        LocalDate from;
        LocalDate to;
        switch (preset) {
            case "today" -> {
                from = today;
                to = today;
            }
            case "week" -> {
                from = today.minusDays(6);
                to = today;
            }
            case "custom" -> {
                from = filter.getDateFrom() != null ? filter.getDateFrom() : today.withDayOfMonth(1);
                to = filter.getDateTo() != null ? filter.getDateTo() : today;
            }
            default -> {
                preset = "month";
                from = today.withDayOfMonth(1);
                to = today;
            }
        }

        String message = null;
        if (from.isAfter(to)) {
            LocalDate tmp = from;
            from = to;
            to = tmp;
            message = "Khoang ngay khong hop le da duoc tu dong dieu chinh.";
        }

        RevenueReportFilter normalized = RevenueReportFilter.builder()
                .dateFrom(from)
                .dateTo(to)
                .preset(preset)
                .branchId(filter.getBranchId())
                .build();
        return new NormalizedFilter(normalized, from.atStartOfDay(), to.plusDays(1).atStartOfDay(), message);
    }

    private static Object[] firstRow(Object value) {
        if (value instanceof Object[] row) {
            return row;
        }
        return new Object[] {0L, 0L, 0L, 0L};
    }

    private static long numberAt(Object[] row, int index) {
        if (row == null || row.length <= index || row[index] == null) {
            return 0L;
        }
        return ((Number) row[index]).longValue();
    }

    private static Integer intAt(Object[] row, int index) {
        if (row == null || row.length <= index || row[index] == null) {
            return null;
        }
        return ((Number) row[index]).intValue();
    }

    private static String stringAt(Object[] row, int index) {
        if (row == null || row.length <= index || row[index] == null) {
            return "";
        }
        return row[index].toString();
    }

    private static LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        return Optional.ofNullable(value)
                .map(Object::toString)
                .map(LocalDate::parse)
                .orElse(LocalDate.now());
    }

    private static LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return Optional.ofNullable(value)
                .map(Object::toString)
                .map(LocalDateTime::parse)
                .orElse(LocalDateTime.now());
    }

    private record NormalizedFilter(RevenueReportFilter filter,
                                    LocalDateTime start,
                                    LocalDateTime end,
                                    String message) {
    }
}
