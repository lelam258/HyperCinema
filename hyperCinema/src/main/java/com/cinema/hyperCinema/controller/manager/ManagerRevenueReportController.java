package com.cinema.hyperCinema.controller.manager;

import com.cinema.hyperCinema.dto.report.revenue.RevenueReportFilter;
import com.cinema.hyperCinema.dto.report.revenue.RevenueReportView;
import com.cinema.hyperCinema.security.CustomUserDetails;
import com.cinema.hyperCinema.service.report.RevenueReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequestMapping("/manager/reports/revenue")
public class ManagerRevenueReportController {

    private final RevenueReportService revenueReportService;

    public ManagerRevenueReportController(RevenueReportService revenueReportService) {
        this.revenueReportService = revenueReportService;
    }

    @GetMapping
    public String revenueReport(@AuthenticationPrincipal CustomUserDetails userDetails,
                                @RequestParam(value = "preset", required = false) String preset,
                                @RequestParam(value = "from", required = false)
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                @RequestParam(value = "to", required = false)
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                Model model) {
        RevenueReportView report = revenueReportService.getManagerReport(
                userDetails != null ? userDetails.getUser() : null,
                RevenueReportFilter.builder()
                        .preset(preset)
                        .dateFrom(from)
                        .dateTo(to)
                        .build());
        model.addAttribute("report", report);
        model.addAttribute("baseUrl", "/manager/reports/revenue");
        model.addAttribute("adminView", false);
        return "manager/reports/revenue";
    }
}
