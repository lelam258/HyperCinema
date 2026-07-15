package com.cinema.hyperCinema.controller.admin;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.cinema.hyperCinema.dto.ui.admin.ActivityLogView;
import com.cinema.hyperCinema.repository.AuditLogRepository;

@Controller
@RequestMapping("/admin/logs")
@PreAuthorize("hasRole('ADMIN')")
public class AdminLogsController {

    private final AuditLogRepository auditLogRepository;

    public AdminLogsController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    public String logs(Model model) {
        model.addAttribute("logs", toActivityLogs(auditLogRepository.findRecentLogs(100)));
        return "admin/logs";
    }

    private List<ActivityLogView> toActivityLogs(List<Object[]> rows) {
        return rows.stream()
                .map(row -> ActivityLogView.builder()
                        .createdAt(asDateTime(row, 0))
                        .actorLabel(asString(row, 1, "System"))
                        .action(asString(row, 2, "Activity"))
                        .description(asString(row, 4, asString(row, 3, "")))
                        .build())
                .toList();
    }

    private String asString(Object[] row, int index, String fallback) {
        if (row == null || row.length <= index || row[index] == null) {
            return fallback;
        }
        return String.valueOf(row[index]);
    }

    private LocalDateTime asDateTime(Object[] row, int index) {
        if (row == null || row.length <= index || !(row[index] instanceof LocalDateTime)) {
            return null;
        }
        return (LocalDateTime) row[index];
    }
}
