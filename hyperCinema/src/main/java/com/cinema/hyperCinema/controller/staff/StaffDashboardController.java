package com.cinema.hyperCinema.controller.staff;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.cinema.hyperCinema.dto.ui.workspace.WorkspaceDashboardView;
import com.cinema.hyperCinema.security.CustomUserDetails;
import com.cinema.hyperCinema.service.ui.BookingUiDataService;
import com.cinema.hyperCinema.service.ui.WorkspaceUiDataService;

@Controller
@RequestMapping("/staff")
public class StaffDashboardController {

    private final WorkspaceUiDataService workspaceUiDataService;
    private final BookingUiDataService bookingUiDataService;

    public StaffDashboardController(WorkspaceUiDataService workspaceUiDataService,
                                    BookingUiDataService bookingUiDataService) {
        this.workspaceUiDataService = workspaceUiDataService;
        this.bookingUiDataService = bookingUiDataService;
    }

    @GetMapping({"/dashboard", ""})
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        addStaffAttributes(userDetails, model);
        return "staff/dashboard";
    }

    @GetMapping("/booking")
    public String bookingWorkspace(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        addStaffAttributes(userDetails, model);
        model.addAttribute("customerFlow", false);
        model.addAttribute("showtimes", bookingUiDataService.upcomingShowtimes(userDetails.getUser(), 20));
        model.addAttribute("foodItems", bookingUiDataService.availableFoodItems(userDetails.getUser()));
        model.addAttribute("posSummary", bookingUiDataService.emptyPosSummary(userDetails.getUser()));
        return "staff/booking";
    }

    private void addStaffAttributes(CustomUserDetails userDetails, Model model) {
        WorkspaceDashboardView workspace = workspaceUiDataService.getStaffDashboard(userDetails.getUser());
        model.addAttribute("workspace", workspace);
        model.addAttribute("staffName", workspace.getActorName());
        model.addAttribute("branchName", workspace.getBranchName());
        model.addAttribute("lastUpdated", workspace.getLastUpdated());
    }
}
