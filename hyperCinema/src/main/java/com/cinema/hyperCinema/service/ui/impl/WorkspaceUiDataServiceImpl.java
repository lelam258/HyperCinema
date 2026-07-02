package com.cinema.hyperCinema.service.ui.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.cinema.hyperCinema.dto.ui.admin.MetricCardView;
import com.cinema.hyperCinema.dto.ui.admin.SeriesPointView;
import com.cinema.hyperCinema.dto.ui.admin.TopMovieView;
import com.cinema.hyperCinema.dto.ui.workspace.CustomerDashboardView;
import com.cinema.hyperCinema.dto.ui.workspace.LeaderboardRowView;
import com.cinema.hyperCinema.dto.ui.workspace.WorkspaceActionView;
import com.cinema.hyperCinema.dto.ui.workspace.WorkspaceDashboardView;
import com.cinema.hyperCinema.model.Branch;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.model.UserMembership;
import com.cinema.hyperCinema.repository.LoyaltyPointRepository;
import com.cinema.hyperCinema.repository.UserMembershipRepository;
import com.cinema.hyperCinema.service.dashboard.BranchManagerDashboardService;
import com.cinema.hyperCinema.service.dashboard.ManagerDashboardService;
import com.cinema.hyperCinema.service.ui.WorkspaceUiDataService;
import com.cinema.hyperCinema.util.UiDisplayMapper;

@Service
public class WorkspaceUiDataServiceImpl implements WorkspaceUiDataService {

    private final LoyaltyPointRepository loyaltyPointRepository;
    private final UserMembershipRepository userMembershipRepository;
    private final ManagerDashboardService managerDashboardService;
    private final BranchManagerDashboardService branchManagerDashboardService;
    private final UiDisplayMapper displayMapper;

    public WorkspaceUiDataServiceImpl(LoyaltyPointRepository loyaltyPointRepository,
                                      UserMembershipRepository userMembershipRepository,
                                      ManagerDashboardService managerDashboardService,
                                      BranchManagerDashboardService branchManagerDashboardService,
                                      UiDisplayMapper displayMapper) {
        this.loyaltyPointRepository = loyaltyPointRepository;
        this.userMembershipRepository = userMembershipRepository;
        this.managerDashboardService = managerDashboardService;
        this.branchManagerDashboardService = branchManagerDashboardService;
        this.displayMapper = displayMapper;
    }
    @Override
    public CustomerDashboardView getCustomerDashboard(User actor) {
        Integer userId = actor != null ? actor.getUserId() : null;
        long points = userId == null ? 0L : Optional.ofNullable(loyaltyPointRepository.sumPointsByUserId(userId)).orElse(0L);
        String membershipTier = userId == null ? "Standard" : userMembershipRepository
                .findFirstByUser_UserIdAndStatusAndEndDateGreaterThanEqualOrderByEndDateDesc(
                        userId, "Active", LocalDate.now())
                .map(UserMembership::getPlan)
                .map(plan -> plan != null ? plan.getName() : "Standard")
                .orElse("Standard");

        return CustomerDashboardView.builder()
                .userId(userId)
                .customerName(actor != null ? actor.getFullName() : "Customer")
                .email(actor != null ? actor.getEmail() : "")
                .phone(actor != null ? actor.getPhone() : "")
                .membershipTier(membershipTier)
                .rewardPoints(points)
                .actions(customerActions())
                .lastUpdated(displayMapper.dateTime(LocalDateTime.now()))
                .build();
    }

    @Override
    public WorkspaceDashboardView getManagerDashboard() {
        return WorkspaceDashboardView.builder()
                .role("manager")
                .actorName("Manager")
                .branchName("Toan he thong")
                .metrics(Arrays.asList(
                        metric("revenue", "Doanh thu", managerDashboardService.sumChainRevenueThisMonth(),
                                "Thang nay", "wallet", true),
                        metric("tickets", "Ve da ban", managerDashboardService.countChainTicketsThisMonth(),
                                "Thang nay", "ticket", false),
                        metric("branches", "Chi nhanh", managerDashboardService.countActiveBranches(),
                                "Dang hoat dong", "building-2", false),
                        metric("movies", "Phim", managerDashboardService.countNowShowingMovies(),
                                "Dang chieu", "film", false)))
                .revenueSeries(series(managerDashboardService.getRevenueLastDays(7), true))
                .leaderboard(leaderboard(managerDashboardService.getBranchLeaderboardThisMonth()))
                .topMovies(topMovies(managerDashboardService.getTopMoviesThisMonth(5)))
                .actions(managerActions())
                .lastUpdated(displayMapper.dateTime(LocalDateTime.now()))
                .build();
    }

    @Override
    public WorkspaceDashboardView getBranchDashboard(User actor) {
        Branch branch = actor == null ? null : actor.getBranch();
        if (branch == null || branch.getBranchId() == null) {
            return emptyBranchDashboard(actor);
        }
        Integer branchId = branch.getBranchId();
        return WorkspaceDashboardView.builder()
                .role("branch")
                .actorName(actor.getFullName())
                .branchId(branchId)
                .branchName(branch.getName())
                .metrics(Arrays.asList(
                        metric("revenue", "Doanh thu", branchManagerDashboardService.sumBranchRevenueThisMonth(branchId),
                                "Thang nay", "wallet", true),
                        metric("tickets", "Ve da ban", branchManagerDashboardService.countBranchTicketsThisMonth(branchId),
                                "Thang nay", "ticket", false),
                        metric("todayBookings", "Dat ve hom nay", branchManagerDashboardService.countBranchBookingsToday(branchId),
                                "Trong ngay", "calendar-check", false)))
                .revenueSeries(series(branchManagerDashboardService.getBranchRevenueLastDays(branchId, 7), true))
                .leaderboard(Collections.emptyList())
                .topMovies(topMovies(branchManagerDashboardService.getBranchTopMovies(branchId, 5)))
                .actions(branchActions(branchId))
                .lastUpdated(displayMapper.dateTime(LocalDateTime.now()))
                .build();
    }

    @Override
    public WorkspaceDashboardView getStaffDashboard(User actor) {
        Branch branch = actor == null ? null : actor.getBranch();
        Integer branchId = branch == null ? null : branch.getBranchId();
        boolean assigned = branchId != null;
        return WorkspaceDashboardView.builder()
                .role("staff")
                .actorName(actor != null ? actor.getFullName() : "Staff")
                .branchId(branchId)
                .branchName(assigned ? branch.getName() : "Chua phan cong chi nhanh")
                .metrics(Collections.emptyList())
                .revenueSeries(Collections.emptyList())
                .leaderboard(Collections.emptyList())
                .topMovies(Collections.emptyList())
                .actions(staffActions(assigned))
                .lastUpdated(displayMapper.dateTime(LocalDateTime.now()))
                .build();
    }

    private WorkspaceDashboardView emptyBranchDashboard(User actor) {
        return WorkspaceDashboardView.builder()
                .role("branch")
                .actorName(actor != null ? actor.getFullName() : "")
                .branchName("Chua phan cong chi nhanh")
                .metrics(Collections.emptyList())
                .revenueSeries(Collections.emptyList())
                .leaderboard(Collections.emptyList())
                .topMovies(Collections.emptyList())
                .actions(branchActions(null))
                .lastUpdated(displayMapper.dateTime(LocalDateTime.now()))
                .build();
    }

    private List<WorkspaceActionView> managerActions() {
        return Arrays.asList(
                action("Chi nhanh", "/admin/branches", "building", true, null),
                action("Phim", "/admin/movies", "film", true, null),
                action("Lich chieu", "/admin/showtimes", "calendar-days", true, null));
    }

    private List<WorkspaceActionView> branchActions(Integer branchId) {
        boolean assigned = branchId != null;
        return Arrays.asList(
                action("Phong chieu", "/admin/halls", "panel-top", assigned, "Chua co chi nhanh"),
                action("So do ghe", "/admin/seats", "armchair", assigned, "Chua co chi nhanh"));
    }

    private List<WorkspaceActionView> staffActions(boolean assigned) {
        return Arrays.asList(
                action("Ban ve", "/staff/booking", "ticket", assigned, "Chua co chi nhanh"),
                action("Tao don F&B", "/admin/food-orders/new", "popcorn", assigned, "Chua co chi nhanh"));
    }

    private List<WorkspaceActionView> customerActions() {
        return Arrays.asList(
                action("Mua ve", "/booking", "ticket", true, null),
                action("Ve cua toi", "/my/bookings", "tickets", true, null));
    }

    private WorkspaceActionView action(String label, String href, String icon, boolean enabled, String disabledReason) {
        return WorkspaceActionView.builder()
                .label(label)
                .href(href)
                .icon(icon)
                .enabled(enabled)
                .disabledReason(enabled ? null : disabledReason)
                .build();
    }

    private MetricCardView metric(String key, String label, long value, String helperText, String icon, boolean currency) {
        return MetricCardView.builder()
                .key(key)
                .label(label)
                .value(value)
                .displayValue(currency ? displayMapper.currency(value) : displayMapper.integer(value))
                .helperText(helperText)
                .icon(icon)
                .build();
    }

    private List<SeriesPointView> series(java.util.Map<String, Long> values, boolean currency) {
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

    private List<LeaderboardRowView> leaderboard(List<Object[]> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return rows.stream()
                .map(row -> LeaderboardRowView.builder()
                        .label(asString(row, 0, "Unknown branch"))
                        .value(asLong(row, 1))
                        .displayValue(displayMapper.currency(asLong(row, 1)))
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
}
