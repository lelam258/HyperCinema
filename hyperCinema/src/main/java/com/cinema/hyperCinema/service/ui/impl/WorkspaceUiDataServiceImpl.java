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

    private final ManagerDashboardService managerDashboardService;
    private final BranchManagerDashboardService branchDashboardService;
    private final LoyaltyPointRepository loyaltyPointRepository;
    private final UserMembershipRepository userMembershipRepository;
    private final UiDisplayMapper displayMapper;

    public WorkspaceUiDataServiceImpl(ManagerDashboardService managerDashboardService,
                                      BranchManagerDashboardService branchDashboardService,
                                      LoyaltyPointRepository loyaltyPointRepository,
                                      UserMembershipRepository userMembershipRepository,
                                      UiDisplayMapper displayMapper) {
        this.managerDashboardService = managerDashboardService;
        this.branchDashboardService = branchDashboardService;
        this.loyaltyPointRepository = loyaltyPointRepository;
        this.userMembershipRepository = userMembershipRepository;
        this.displayMapper = displayMapper;
    }

    @Override
    public WorkspaceDashboardView getManagerDashboard() {
        return WorkspaceDashboardView.builder()
                .role("manager")
                .metrics(Arrays.asList(
                        metric("revenue", "Doanh thu", managerDashboardService.sumChainRevenueThisMonth(),
                                "Trong thang", "banknote"),
                        metric("tickets", "Ve da ban", managerDashboardService.countChainTicketsThisMonth(),
                                "Trong thang", "ticket"),
                        metric("branches", "Chi nhanh", managerDashboardService.countActiveBranches(),
                                "Dang hoat dong", "building-2"),
                        metric("movies", "Phim", managerDashboardService.countNowShowingMovies(),
                                "Dang chieu", "film")))
                .revenueSeries(managerDashboardService.getRevenueLastDays(14).entrySet().stream()
                        .map(entry -> series(entry.getKey(), entry.getValue(), true))
                        .collect(Collectors.toList()))
                .leaderboard(managerDashboardService.getBranchLeaderboardThisMonth().stream()
                        .map(row -> LeaderboardRowView.builder()
                                .label(asString(row, 0, "Branch"))
                                .value(asLong(row, 1))
                                .displayValue(displayMapper.currency(asLong(row, 1)))
                                .build())
                        .collect(Collectors.toList()))
                .topMovies(toTopMovies(managerDashboardService.getTopMoviesThisMonth(5)))
                .actions(managerActions())
                .lastUpdated(displayMapper.dateTime(LocalDateTime.now()))
                .build();
    }

    @Override
    public WorkspaceDashboardView getBranchDashboard(User actor) {
        Branch branch = actor != null ? actor.getBranch() : null;
        if (branch == null) {
            return emptyBranchDashboard(actor);
        }
        Integer branchId = branch.getBranchId();
        return WorkspaceDashboardView.builder()
                .role("branch")
                .actorName(actor.getFullName())
                .branchId(branchId)
                .branchName(branch.getName())
                .metrics(Arrays.asList(
                        metric("revenue", "Doanh thu", branchDashboardService.sumBranchRevenueThisMonth(branchId),
                                "Trong thang", "banknote"),
                        metric("tickets", "Ve da ban", branchDashboardService.countBranchTicketsThisMonth(branchId),
                                "Trong thang", "ticket"),
                        metric("todayBookings", "Booking hom nay", branchDashboardService.countBranchBookingsToday(branchId),
                                "Tai chi nhanh", "calendar-check")))
                .revenueSeries(branchDashboardService.getBranchRevenueLastDays(branchId, 14).entrySet().stream()
                        .map(entry -> series(entry.getKey(), entry.getValue(), true))
                        .collect(Collectors.toList()))
                .topMovies(toTopMovies(branchDashboardService.getBranchTopMovies(branchId, 5)))
                .actions(branchActions(branchId))
                .lastUpdated(displayMapper.dateTime(LocalDateTime.now()))
                .build();
    }

    @Override
    public WorkspaceDashboardView getStaffDashboard(User actor) {
        Branch branch = actor != null ? actor.getBranch() : null;
        boolean assigned = branch != null;
        return WorkspaceDashboardView.builder()
                .role("staff")
                .actorName(actor != null ? actor.getFullName() : "Staff")
                .branchId(assigned ? branch.getBranchId() : null)
                .branchName(assigned ? branch.getName() : "Chua phan cong chi nhanh")
                .metrics(Collections.emptyList())
                .actions(staffActions(assigned))
                .lastUpdated(displayMapper.dateTime(LocalDateTime.now()))
                .build();
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

    private WorkspaceDashboardView emptyBranchDashboard(User actor) {
        return WorkspaceDashboardView.builder()
                .role("branch")
                .actorName(actor != null ? actor.getFullName() : "")
                .branchName("Chua phan cong chi nhanh")
                .metrics(Collections.emptyList())
                .revenueSeries(Collections.emptyList())
                .topMovies(Collections.emptyList())
                .actions(branchActions(null))
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

    private SeriesPointView series(String label, Long value, boolean currency) {
        long safeValue = value == null ? 0L : value;
        return SeriesPointView.builder()
                .label(label)
                .value(safeValue)
                .displayValue(currency ? displayMapper.currency(safeValue) : displayMapper.integer(safeValue))
                .build();
    }

    private List<TopMovieView> toTopMovies(List<Object[]> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return rows.stream()
                .map(row -> TopMovieView.builder()
                        .title(asString(row, 0, "Movie"))
                        .genre("Bookings")
                        .bookingCount(asLong(row, 1))
                        .build())
                .collect(Collectors.toList());
    }

    private List<WorkspaceActionView> managerActions() {
        return Arrays.asList(
                action("Quan ly phim", "/admin/movies", "clapperboard", true, null),
                action("Quan ly chi nhanh", "/admin/branches", "building-2", true, null));
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
