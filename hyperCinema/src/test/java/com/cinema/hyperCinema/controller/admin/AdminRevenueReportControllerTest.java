package com.cinema.hyperCinema.controller.admin;

import com.cinema.hyperCinema.dto.report.revenue.RevenueReportFilter;
import com.cinema.hyperCinema.dto.report.revenue.RevenueReportView;
import com.cinema.hyperCinema.model.Branch;
import com.cinema.hyperCinema.repository.BranchRepository;
import com.cinema.hyperCinema.service.report.RevenueReportService;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminRevenueReportControllerTest {

    @Test
    void revenueReportAddsAdminReportAndBranchOptionsToModel() {
        RevenueReportService service = mock(RevenueReportService.class);
        BranchRepository branchRepository = mock(BranchRepository.class);
        AdminRevenueReportController controller = new AdminRevenueReportController(service, branchRepository);
        RevenueReportView report = RevenueReportView.builder().scopeRole("ADMIN").build();
        Branch branch = new Branch();
        branch.setBranchId(1);
        branch.setName("HyperCinema Q1");
        when(service.getAdminReport(any(RevenueReportFilter.class))).thenReturn(report);
        when(branchRepository.findByStatusIgnoreCaseOrderByNameAsc("Active")).thenReturn(List.of(branch));
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.revenueReport("month", null, null, null, model);

        assertThat(view).isEqualTo("admin/reports/revenue");
        assertThat(model.get("report")).isSameAs(report);
        assertThat(model.get("branches")).isEqualTo(List.of(branch));
        assertThat(model.get("adminView")).isEqualTo(true);
    }
}
