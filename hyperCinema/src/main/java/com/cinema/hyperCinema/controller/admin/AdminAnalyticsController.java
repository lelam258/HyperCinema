package com.cinema.hyperCinema.controller.admin;

import com.cinema.hyperCinema.model.Branch;
import com.cinema.hyperCinema.repository.BranchRepository;
import com.cinema.hyperCinema.service.AnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/analytics")
public class AdminAnalyticsController {

    private final AnalyticsService analyticsService;
    private final BranchRepository branchRepository;

    public AdminAnalyticsController(AnalyticsService analyticsService, BranchRepository branchRepository) {
        this.analyticsService = analyticsService;
        this.branchRepository = branchRepository;
    }

    @GetMapping
    public String getAnalyticsPage(
            @RequestParam(value = "tab", defaultValue = "ticket") String tab,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "branchId", required = false) Integer branchId,
            Model model) {

        // Default dates: last 30 days
        if (to == null) {
            to = LocalDate.now();
        }
        if (from == null) {
            from = to.minusDays(30);
        }

        // Fetch report based on tab
        Map<String, Object> report;
        if ("fb".equalsIgnoreCase(tab)) {
            report = analyticsService.getFoodSalesReport(from, to, branchId);
        } else if ("occupancy".equalsIgnoreCase(tab)) {
            report = analyticsService.getHallOccupancyReport(from, to, branchId);
        } else {
            tab = "ticket";
            report = analyticsService.getTicketSalesReport(from, to, branchId);
        }

        List<Branch> branches = branchRepository.findAll();

        model.addAttribute("tab", tab);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("branchId", branchId);
        model.addAttribute("branches", branches);
        model.addAttribute("report", report);
        model.addAttribute("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));

        return "admin/analytics";
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportAnalyticsReport(
            @RequestParam(value = "type", defaultValue = "ticket") String type,
            @RequestParam(value = "from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "branchId", required = false) Integer branchId,
            @RequestParam(value = "format", defaultValue = "csv") String format) {

        byte[] fileBytes = analyticsService.exportReport(type, from, to, branchId, format);

        String fileExtension = "csv";
        MediaType mediaType = MediaType.parseMediaType("text/csv");

        if ("pdf".equalsIgnoreCase(format)) {
            fileExtension = "pdf";
            mediaType = MediaType.parseMediaType("application/pdf");
        } else if ("excel".equalsIgnoreCase(format)) {
            fileExtension = "csv"; // CSV is used as the Excel-compatible download format
            mediaType = MediaType.parseMediaType("text/csv");
        }

        String filename = String.format("%s_report_%s_to_%s.%s", type, from, to, fileExtension);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(mediaType)
                .body(fileBytes);
    }
}
