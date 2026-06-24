package com.cinema.hyperCinema.controller.ui;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cinema.hyperCinema.dto.admin.movie.response.MovieDetailView;
import com.cinema.hyperCinema.dto.ui.booking.SeatAvailabilityView;
import com.cinema.hyperCinema.dto.ui.workspace.CustomerDashboardView;
import com.cinema.hyperCinema.model.Branch;
import com.cinema.hyperCinema.model.Hall;
import com.cinema.hyperCinema.model.Movie;
import com.cinema.hyperCinema.model.Showtime;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.security.CustomUserDetails;
import com.cinema.hyperCinema.service.booking.BookingService;
import com.cinema.hyperCinema.service.movie.MovieService;
import com.cinema.hyperCinema.service.ui.BookingUiDataService;
import com.cinema.hyperCinema.service.ui.WorkspaceUiDataService;
import com.cinema.hyperCinema.util.SeatPricing;

@Controller
public class BookingPageController {

    private final BookingUiDataService bookingUiDataService;
    private final WorkspaceUiDataService workspaceUiDataService;
    private final MovieService movieService;
    private final BookingService bookingService;

    public BookingPageController(BookingUiDataService bookingUiDataService,
                                 WorkspaceUiDataService workspaceUiDataService,
                                 MovieService movieService,
                                 BookingService bookingService) {
        this.bookingUiDataService = bookingUiDataService;
        this.workspaceUiDataService = workspaceUiDataService;
        this.movieService = movieService;
        this.bookingService = bookingService;
    }

    @GetMapping("/booking")
    public String booking(@AuthenticationPrincipal CustomUserDetails userDetails,
                          @RequestParam(required = false) Integer showtimeId,
                          Model model) {
        var user = userDetails.getUser();
        boolean customerFlow = userDetails.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_CUSTOMER".equals(authority.getAuthority()));
        if (customerFlow) {
            addCustomerModel(user, model);
            if (showtimeId == null) {
                return "redirect:/my/dashboard";
            }
        }
        model.addAttribute("customerFlow", customerFlow);
        model.addAttribute("staffName", user.getFullName());
        model.addAttribute("branchName", user.getBranch() != null ? user.getBranch().getName() : "Tat ca chi nhanh");
        model.addAttribute("showtimes", bookingUiDataService.upcomingShowtimes(user, 20));
        model.addAttribute("foodItems", bookingUiDataService.availableFoodItems(user));
        model.addAttribute("posSummary", bookingUiDataService.emptyPosSummary(user));
        if (showtimeId != null) {
            var selectedShowtime = bookingService.findShowtimeWithDetails(showtimeId);
            if (selectedShowtime.isPresent()) {
                addSelectedShowtimeModel(selectedShowtime.get(), user, model);
            } else if (customerFlow) {
                return "redirect:/my/dashboard";
            }
        }
        return customerFlow ? "my/booking" : "staff/booking";
    }

    @GetMapping("/booking/movies/{movieId}")
    public String movieBooking(@PathVariable Integer movieId,
                               @RequestParam(required = false)
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                               @RequestParam(required = false) String city,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               Model model) {
        User user = userDetails.getUser();
        addCustomerModel(user, model);

        MovieDetailView movie = movieService.findById(movieId);
        LocalDate selectedDate = date != null ? date : LocalDate.now();
        List<Showtime> upcomingShowtimes = bookingService.findUpcomingShowtimesForMovie(movieId);
        List<String> cities = upcomingShowtimes.stream()
                .map(showtime -> showtime.getHall().getBranch().getCity())
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        String selectedCity = city != null && !city.isBlank() ? city : null;

        List<BranchScheduleView> branchSchedules = groupBranchSchedules(
                upcomingShowtimes, selectedDate, selectedCity);
        List<DateTabView> dateTabs = buildDateTabs(movieId, selectedDate, selectedCity, upcomingShowtimes);

        model.addAttribute("movie", movie);
        model.addAttribute("dateTabs", dateTabs);
        model.addAttribute("cities", cities);
        model.addAttribute("selectedCity", selectedCity);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("branchSchedules", branchSchedules);
        model.addAttribute("hasShowtimes", !branchSchedules.isEmpty());
        return "my/movie-detail";
    }

    @PostMapping("/booking")
    public String createBooking(@AuthenticationPrincipal CustomUserDetails userDetails,
                                @RequestParam Integer showtimeId,
                                @RequestParam(name = "seatIds", required = false) List<Integer> seatIds,
                                @RequestParam(name = "foodItemIds", required = false) List<Integer> foodItemIds,
                                @RequestParam(name = "foodQuantities", required = false) List<Integer> foodQuantities,
                                RedirectAttributes redirectAttributes) {
        User user = userDetails.getUser();
        try {
            var savedBooking = bookingService.createPendingVietQrBooking(
                    user, showtimeId, seatIds, foodItemIds, foodQuantities);
            return "redirect:/payment/vietqr/" + savedBooking.getBookingId();
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("bookingError", ex.getMessage());
            return "redirect:/my/dashboard";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("bookingError", ex.getMessage());
            return "redirect:/booking?showtimeId=" + showtimeId;
        }
    }

    private void addCustomerModel(User user, Model model) {
        CustomerDashboardView dashboard = workspaceUiDataService.getCustomerDashboard(user);
        model.addAttribute("customerName", dashboard.getCustomerName());
        model.addAttribute("membershipTier", dashboard.getMembershipTier());
        model.addAttribute("rewardPoints", dashboard.getRewardPoints());
    }

    private void addSelectedShowtimeModel(Showtime showtime, User user, Model model) {
        ShowtimeSummaryView summary = ShowtimeSummaryView.from(showtime);
        List<SeatAvailabilityView> seats = bookingUiDataService.seatAvailability(
                showtime.getShowtimeId(), user);
        model.addAttribute("selectedShowtime", summary);
        model.addAttribute("seats", seats);
    }

    private List<DateTabView> buildDateTabs(Integer movieId,
                                            LocalDate selectedDate,
                                            String selectedCity,
                                            List<Showtime> showtimes) {
        LocalDate today = LocalDate.now();
        List<DateTabView> tabs = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate tabDate = today.plusDays(i);
            boolean hasShowtime = showtimes.stream()
                    .anyMatch(showtime -> tabDate.equals(showtime.getStartTime().toLocalDate()));
            String label = i == 0 ? "Hom nay" : (i == 1 ? "Ngay mai" : dayLabel(tabDate));
            tabs.add(new DateTabView(
                    movieId,
                    tabDate,
                    label,
                    tabDate.format(DateTimeFormatter.ofPattern("dd/MM")),
                    selectedCity,
                    tabDate.equals(selectedDate),
                    hasShowtime));
        }
        return tabs;
    }

    private List<BranchScheduleView> groupBranchSchedules(List<Showtime> showtimes,
                                                          LocalDate selectedDate,
                                                          String selectedCity) {
        Map<Integer, BranchScheduleView> grouped = new LinkedHashMap<>();
        showtimes.stream()
                .filter(showtime -> selectedDate.equals(showtime.getStartTime().toLocalDate()))
                .filter(showtime -> selectedCity == null
                        || selectedCity.equals(showtime.getHall().getBranch().getCity()))
                .sorted(Comparator.comparing(Showtime::getStartTime))
                .forEach(showtime -> {
                    Branch branch = showtime.getHall().getBranch();
                    BranchScheduleView branchView = grouped.computeIfAbsent(
                            branch.getBranchId(),
                            ignored -> BranchScheduleView.from(branch));
                    branchView.getShowtimes().add(ShowtimeSummaryView.from(showtime));
                });
        return new ArrayList<>(grouped.values());
    }

    private String dayLabel(LocalDate date) {
        return date.getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH);
    }

    public static class DateTabView {
        private final Integer movieId;
        private final LocalDate date;
        private final String label;
        private final String displayDate;
        private final String city;
        private final boolean selected;
        private final boolean hasShowtime;

        public DateTabView(Integer movieId,
                           LocalDate date,
                           String label,
                           String displayDate,
                           String city,
                           boolean selected,
                           boolean hasShowtime) {
            this.movieId = movieId;
            this.date = date;
            this.label = label;
            this.displayDate = displayDate;
            this.city = city;
            this.selected = selected;
            this.hasShowtime = hasShowtime;
        }

        public Integer getMovieId() { return movieId; }
        public LocalDate getDate() { return date; }
        public String getIsoDate() { return date.toString(); }
        public String getLabel() { return label; }
        public String getDisplayDate() { return displayDate; }
        public String getCity() { return city; }
        public boolean isSelected() { return selected; }
        public boolean getHasShowtime() { return hasShowtime; }
        public boolean isHasShowtime() { return hasShowtime; }
    }

    public static class BranchScheduleView {
        private final Integer branchId;
        private final String name;
        private final String address;
        private final String city;
        private final List<ShowtimeSummaryView> showtimes = new ArrayList<>();

        private BranchScheduleView(Integer branchId, String name, String address, String city) {
            this.branchId = branchId;
            this.name = name;
            this.address = address;
            this.city = city;
        }

        public static BranchScheduleView from(Branch branch) {
            return new BranchScheduleView(
                    branch.getBranchId(),
                    branch.getName(),
                    branch.getAddress(),
                    branch.getCity());
        }

        public Integer getBranchId() { return branchId; }
        public String getName() { return name; }
        public String getAddress() { return address; }
        public String getCity() { return city; }
        public List<ShowtimeSummaryView> getShowtimes() { return showtimes; }
    }

    public static class ShowtimeSummaryView {
        private final Integer showtimeId;
        private final Integer movieId;
        private final String movieTitle;
        private final String posterUrl;
        private final String branchName;
        private final String branchCity;
        private final String hallName;
        private final String hallType;
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;
        private final Integer price;

        private ShowtimeSummaryView(Integer showtimeId,
                                    Integer movieId,
                                    String movieTitle,
                                    String posterUrl,
                                    String branchName,
                                    String branchCity,
                                    String hallName,
                                    String hallType,
                                    LocalDateTime startTime,
                                    LocalDateTime endTime,
                                    Integer price) {
            this.showtimeId = showtimeId;
            this.movieId = movieId;
            this.movieTitle = movieTitle;
            this.posterUrl = posterUrl;
            this.branchName = branchName;
            this.branchCity = branchCity;
            this.hallName = hallName;
            this.hallType = hallType;
            this.startTime = startTime;
            this.endTime = endTime;
            this.price = price;
        }

        public static ShowtimeSummaryView from(Showtime showtime) {
            Movie movie = showtime.getMovie();
            Hall hall = showtime.getHall();
            Branch branch = hall.getBranch();
            return new ShowtimeSummaryView(
                    showtime.getShowtimeId(),
                    movie.getMovieId(),
                    movie.getTitle(),
                    movie.getPosterUrl(),
                    branch.getName(),
                    branch.getCity(),
                    hall.getName(),
                    hall.getHallType(),
                    showtime.getStartTime(),
                    showtime.getEndTime(),
                    SeatPricing.priceFor("STANDARD"));
        }

        public Integer getShowtimeId() { return showtimeId; }
        public Integer getMovieId() { return movieId; }
        public String getMovieTitle() { return movieTitle; }
        public String getPosterUrl() { return posterUrl; }
        public String getBranchName() { return branchName; }
        public String getBranchCity() { return branchCity; }
        public String getHallName() { return hallName; }
        public String getHallType() { return hallType; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public Integer getPrice() { return price; }
    }
}
