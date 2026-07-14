package com.cinema.hyperCinema.controller.admin;

import com.cinema.hyperCinema.dto.report.revenue.RevenueReportFilter;
import com.cinema.hyperCinema.dto.report.revenue.RevenueReportView;
import com.cinema.hyperCinema.repository.BranchRepository;
import com.cinema.hyperCinema.service.report.RevenueReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin/reports/revenue")
public class AdminRevenueReportController {

    private final RevenueReportService revenueReportService;
    private final BranchRepository branchRepository;

    public AdminRevenueReportController(RevenueReportService revenueReportService,
                                        BranchRepository branchRepository) {
        this.revenueReportService = revenueReportService;
        this.branchRepository = branchRepository;
    }

    @GetMapping
    public String revenueReport(@RequestParam(value = "preset", required = false) String preset,
                                @RequestParam(value = "from", required = false)
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                @RequestParam(value = "to", required = false)
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                @RequestParam(value = "branchId", required = false) Integer branchId,
                                Model model) {
        RevenueReportView report = revenueReportService.getAdminReport(RevenueReportFilter.builder()
                .preset(preset)
                .dateFrom(from)
                .dateTo(to)
                .branchId(branchId)
                .build());
        model.addAttribute("report", report);
        model.addAttribute("branches", branchRepository.findByStatusIgnoreCaseOrderByNameAsc("Active"));
        model.addAttribute("baseUrl", "/admin/reports/revenue");
        model.addAttribute("adminView", true);
        return "admin/reports/revenue";
    }
}
