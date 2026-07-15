package com.cinema.hyperCinema.controller.staff;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.cinema.hyperCinema.dto.ui.booking.ShowtimeOptionView;
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
        boolean hasBranch = userDetails.getUser().getBranch() != null;
        List<ShowtimeOptionView> showtimes = hasBranch
                ? bookingUiDataService.upcomingShowtimes(userDetails.getUser(), 50)
                : List.of();
        model.addAttribute("customerFlow", false);
        model.addAttribute("hasBranch", hasBranch);
        model.addAttribute("branchId", hasBranch ? userDetails.getUser().getBranch().getBranchId() : null);
        model.addAttribute("showtimes", showtimes);
        model.addAttribute("posShowtimeGroups", groupShowtimes(showtimes));
        model.addAttribute("foodItems", hasBranch ? bookingUiDataService.availableFoodItems(userDetails.getUser()) : List.of());
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

    private List<PosMovieShowtimeGroup> groupShowtimes(List<ShowtimeOptionView> showtimes) {
        Map<String, PosMovieShowtimeGroup> groups = new LinkedHashMap<>();
        showtimes.stream()
                .sorted(Comparator.comparing(ShowtimeOptionView::getMovieTitle)
                        .thenComparing(ShowtimeOptionView::getStartTime))
                .forEach(showtime -> {
                    String key = showtime.getMovieTitle() + "::" + showtime.getFormatLabel();
                    PosMovieShowtimeGroup group = groups.computeIfAbsent(key,
                            ignored -> new PosMovieShowtimeGroup(
                                    showtime.getMovieTitle(),
                                    showtime.getFormatLabel(),
                                    showtime.getStartTime() != null
                                            && LocalDate.now().equals(showtime.getStartTime().toLocalDate())));
                    group.getShowtimes().add(showtime);
                });
        return new ArrayList<>(groups.values());
    }

    public static class PosMovieShowtimeGroup {
        private final String movieTitle;
        private final String formatLabel;
        private final boolean today;
        private final List<ShowtimeOptionView> showtimes = new ArrayList<>();

        public PosMovieShowtimeGroup(String movieTitle, String formatLabel, boolean today) {
            this.movieTitle = movieTitle;
            this.formatLabel = formatLabel;
            this.today = today;
        }

        public String getMovieTitle() {
            return movieTitle;
        }

        public String getFormatLabel() {
            return formatLabel;
        }

        public boolean isToday() {
            return today;
        }

        public List<ShowtimeOptionView> getShowtimes() {
            return showtimes;
        }
    }
}
