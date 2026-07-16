package com.cinema.hyperCinema.controller.manager;

import com.cinema.hyperCinema.dto.report.revenue.RevenueReportFilter;
import com.cinema.hyperCinema.dto.report.revenue.RevenueReportView;
import com.cinema.hyperCinema.model.Branch;
import com.cinema.hyperCinema.model.Role;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.security.CustomUserDetails;
import com.cinema.hyperCinema.service.report.RevenueReportService;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ManagerRevenueReportControllerTest {

    @Test
    void revenueReportUsesAuthenticatedManagerAndDoesNotAcceptBranchParameter() {
        RevenueReportService service = mock(RevenueReportService.class);
        ManagerRevenueReportController controller = new ManagerRevenueReportController(service);
        User manager = manager();
        RevenueReportView report = RevenueReportView.builder()
                .scopeRole("MANAGER")
                .branchId(manager.getBranch().getBranchId())
                .build();
        when(service.getManagerReport(eq(manager), any(RevenueReportFilter.class))).thenReturn(report);
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.revenueReport(new CustomUserDetails(manager), "month", null, null, model);

        assertThat(view).isEqualTo("manager/reports/revenue");
        assertThat(model.get("report")).isSameAs(report);
        assertThat(model.get("adminView")).isEqualTo(false);
    }

    private static User manager() {
        Role role = new Role();
        role.setName("Manager");
        Branch branch = new Branch();
        branch.setBranchId(7);
        branch.setName("HyperCinema Q1");
        User user = new User();
        user.setUserId(21);
        user.setUsername("manager");
        user.setPasswordHash("x");
        user.setStatus("Active");
        user.setRole(role);
        user.setBranch(branch);
        return user;
    }
}
