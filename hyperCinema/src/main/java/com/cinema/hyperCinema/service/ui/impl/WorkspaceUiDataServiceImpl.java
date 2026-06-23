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
    private final UiDisplayMapper displayMapper;

    public WorkspaceUiDataServiceImpl(LoyaltyPointRepository loyaltyPointRepository,
                                      UserMembershipRepository userMembershipRepository,
                                      UiDisplayMapper displayMapper) {
        this.loyaltyPointRepository = loyaltyPointRepository;
        this.userMembershipRepository = userMembershipRepository;
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
}
