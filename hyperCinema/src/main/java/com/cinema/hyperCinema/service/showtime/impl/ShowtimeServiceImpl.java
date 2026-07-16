package com.cinema.hyperCinema.service.showtime.impl;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;

import com.cinema.hyperCinema.dto.admin.hall.response.BranchOption;
import com.cinema.hyperCinema.dto.admin.hall.response.HallListItem;
import com.cinema.hyperCinema.dto.admin.hall.response.SeatTypePriceView;
import com.cinema.hyperCinema.dto.admin.showtime.request.ShowtimeCreateRequest;
import com.cinema.hyperCinema.dto.admin.showtime.request.ShowtimeSearchCriteria;
import com.cinema.hyperCinema.dto.admin.showtime.request.ShowtimeUpdateRequest;
import com.cinema.hyperCinema.dto.admin.showtime.response.MovieOption;
import com.cinema.hyperCinema.dto.admin.showtime.response.ShowtimeDetailView;
import com.cinema.hyperCinema.dto.admin.showtime.response.ShowtimeListItem;
import com.cinema.hyperCinema.dto.admin.showtime.response.ShowtimeManagementContext;
import com.cinema.hyperCinema.exception.showtime.ShowtimeNotFoundException;
import com.cinema.hyperCinema.exception.showtime.ShowtimeValidationException;
import com.cinema.hyperCinema.model.Branch;
import com.cinema.hyperCinema.model.Hall;
import com.cinema.hyperCinema.model.Movie;
import com.cinema.hyperCinema.model.Role;
import com.cinema.hyperCinema.model.Showtime;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.repository.BookingRepository;
import com.cinema.hyperCinema.repository.BranchMovieRepository;
import com.cinema.hyperCinema.repository.BranchRepository;
import com.cinema.hyperCinema.repository.HallRepository;
import com.cinema.hyperCinema.repository.MovieRepository;
import com.cinema.hyperCinema.repository.PaymentRepository;
import com.cinema.hyperCinema.repository.SeatReservationRepository;
import com.cinema.hyperCinema.repository.ShowtimeRepository;
import com.cinema.hyperCinema.repository.TicketRepository;
import com.cinema.hyperCinema.repository.UserRepository;
import com.cinema.hyperCinema.service.pricing.HallSeatTypePricingService;
import com.cinema.hyperCinema.service.showtime.ShowtimeService;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ShowtimeServiceImpl implements ShowtimeService {

    private static final String STATUS_ACTIVE = "Active";
    private static final String SHOWTIME_ACTIVE = "ACTIVE";
    private static final String SHOWTIME_CANCELLED = "CANCELLED";

    private final ShowtimeRepository showtimeRepository;
    private final MovieRepository movieRepository;
    private final BranchRepository branchRepository;
    private final HallRepository hallRepository;
    private final BranchMovieRepository branchMovieRepository;
    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final SeatReservationRepository seatReservationRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final HallSeatTypePricingService hallSeatTypePricingService;

    @Override
    @Transactional(readOnly = true)
    public Page<ShowtimeListItem> search(ShowtimeSearchCriteria criteria, Pageable pageable, User actor) {
        User current = loadActor(actor);
        Integer scopedBranchId = resolveSearchBranchId(criteria.getBranchId(), current);
        Integer hallId = resolveSearchHallId(criteria.getHallId(), scopedBranchId, current);
        LocalDateTime startFrom = criteria.getDateFrom() == null
                ? null
                : criteria.getDateFrom().atStartOfDay();
        LocalDateTime startTo = criteria.getDateTo() == null
                ? null
                : criteria.getDateTo().atTime(LocalTime.MAX);
        LocalDateTime now = LocalDateTime.now();
        if ("UPCOMING".equals(criteria.getTimeState()) && startFrom == null) {
            startFrom = now;
        }
        if ("PAST".equals(criteria.getTimeState()) && startTo == null) {
            startTo = now;
        }
        return showtimeRepository.searchManaged(
                normalizeOptional(criteria.getKeyword()),
                criteria.getMovieId(),
                scopedBranchId,
                hallId,
                startFrom,
                startTo,
                pageable).map(this::toListItem);
    }

    @Override
    @Transactional(readOnly = true)
    public ShowtimeDetailView findById(Integer showtimeId, User actor) {
        User current = loadActor(actor);
        Showtime showtime = loadShowtime(showtimeId);
        assertCanManageBranch(current, showtimeBranchId(showtime));
        return toDetailView(showtime);
    }

    @Override
    public ShowtimeDetailView create(ShowtimeCreateRequest request, User actor) {
        User current = loadActor(actor);
        Movie movie = loadMovie(request.getMovieId());
        ensureMovieSchedulable(movie);
        Hall hall = loadHall(request.getHallId());
        ensureHallActive(hall);
        Integer branchId = resolveTargetBranchId(request.getBranchId(), hall, current);
        validateMovieAssignment(movie.getMovieId(), branchId);
        validateCreateOrUpdate(null, hall.getHallId(), request.getStartTime(), request.getEndTime());

        Showtime showtime = new Showtime();
        showtime.setMovie(movie);
        showtime.setHall(hall);
        showtime.setStartTime(request.getStartTime());
        showtime.setEndTime(request.getEndTime());
        showtime.setPrice(ticketPriceFor(hall));
        showtime.setStatus(SHOWTIME_ACTIVE);
        return toDetailView(showtimeRepository.save(showtime));
    }

    @Override
    public ShowtimeDetailView update(Integer showtimeId, ShowtimeUpdateRequest request, User actor) {
        User current = loadActor(actor);
        Showtime showtime = loadShowtime(showtimeId);
        assertCanManageBranch(current, showtimeBranchId(showtime));
        ensureEditable(showtimeId, showtime);

        Movie movie = loadMovie(request.getMovieId());
        ensureMovieSchedulable(movie);
        Hall hall = loadHall(request.getHallId());
        ensureHallActive(hall);
        Integer branchId = resolveTargetBranchId(request.getBranchId(), hall, current);
        validateMovieAssignment(movie.getMovieId(), branchId);
        validateCreateOrUpdate(showtimeId, hall.getHallId(), request.getStartTime(), request.getEndTime());

        showtime.setMovie(movie);
        showtime.setHall(hall);
        showtime.setStartTime(request.getStartTime());
        showtime.setEndTime(request.getEndTime());
        showtime.setPrice(ticketPriceFor(hall));
        showtime.setStatus(SHOWTIME_ACTIVE);
        return toDetailView(showtimeRepository.save(showtime));
    }

    @Override
    public void delete(Integer showtimeId, User actor) {
        User current = loadActor(actor);
        Showtime showtime = loadShowtime(showtimeId);
        assertCanManageBranch(current, showtimeBranchId(showtime));
        ensureNoDependencies(showtimeId);
        showtime.setStatus(SHOWTIME_CANCELLED);
        showtimeRepository.save(showtime);
    }

    @Override
    @Transactional(readOnly = true)
    public ShowtimeManagementContext managementContext(User actor) {
        User current = loadActor(actor);
        boolean admin = isAdmin(current);
        BranchOption lockedBranch = null;
        Integer scopedBranchId = null;
        if (!admin && current.getBranch() != null) {
            lockedBranch = toBranchOption(current.getBranch());
            scopedBranchId = current.getBranch().getBranchId();
        }
        List<BranchOption> branches = admin
                ? branchRepository.findByStatusIgnoreCase("Active", Sort.by(Sort.Direction.ASC, "name")).stream()
                        .map(this::toBranchOption)
                        .toList()
                : List.of();
        List<HallListItem> halls = (admin ? hallRepository.findByStatusIgnoreCaseOrderByNameAsc(STATUS_ACTIVE)
                : (scopedBranchId == null ? List.<Hall>of()
                        : hallRepository.findByBranch_BranchIdAndStatusIgnoreCase(scopedBranchId, STATUS_ACTIVE)))
                .stream()
                .map(this::toHallOption)
                .toList();
        List<MovieOption> movies = movieRepository.findByStatusNotIgnoreCase(
                        "Ended",
                        Sort.by(Sort.Direction.ASC, "title")).stream()
                .map(this::toMovieOption)
                .toList();
        return ShowtimeManagementContext.builder()
                .admin(admin)
                .sidebar(sidebarFor(current))
                .lockedBranch(lockedBranch)
                .branchOptions(branches)
                .hallOptions(halls)
                .movieOptions(movies)
                .build();
    }

    private void validateCreateOrUpdate(Integer showtimeId, Integer hallId, LocalDateTime startTime,
                                        LocalDateTime endTime) {
        if (startTime == null) {
            throw new ShowtimeValidationException("showtime.start.required");
        }
        if (endTime == null) {
            throw new ShowtimeValidationException("showtime.end.required");
        }
        if (!endTime.isAfter(startTime)) {
            throw new ShowtimeValidationException("showtime.time.invalid");
        }
        boolean overlap = showtimeId == null
                ? showtimeRepository.existsOverlap(hallId, startTime, endTime)
                : showtimeRepository.existsOverlapExcludingShowtime(hallId, showtimeId, startTime, endTime);
        if (overlap) {
            throw new ShowtimeValidationException("showtime.time.overlap");
        }
    }

    private User loadActor(User actor) {
        if (actor == null || actor.getUserId() == null) {
            throw new ShowtimeValidationException("showtime.access.denied");
        }
        return userRepository.findById(actor.getUserId())
                .orElseThrow(() -> new ShowtimeValidationException("showtime.access.denied"));
    }

    private Showtime loadShowtime(Integer showtimeId) {
        return showtimeRepository.findByIdWithMovieHallAndBranch(showtimeId)
                .orElseThrow(() -> new ShowtimeNotFoundException(showtimeId));
    }

    private Movie loadMovie(Integer movieId) {
        if (movieId == null) {
            throw new ShowtimeValidationException("showtime.movie.required");
        }
        return movieRepository.findById(movieId)
                .orElseThrow(() -> new ShowtimeValidationException("showtime.movie.required"));
    }

    private Hall loadHall(Integer hallId) {
        if (hallId == null) {
            throw new ShowtimeValidationException("showtime.hall.required");
        }
        return hallRepository.findById(hallId)
                .orElseThrow(() -> new ShowtimeValidationException("showtime.hall.required"));
    }

    private void ensureHallActive(Hall hall) {
        if (!STATUS_ACTIVE.equalsIgnoreCase(hall.getStatus())) {
            throw new ShowtimeValidationException("showtime.hall.inactive");
        }
        Branch branch = hall.getBranch();
        if (branch == null || !STATUS_ACTIVE.equalsIgnoreCase(branch.getStatus())) {
            throw new ShowtimeValidationException("showtime.branch.inactive");
        }
    }

    private void ensureMovieSchedulable(Movie movie) {
        if ("Ended".equalsIgnoreCase(movie.getStatus())) {
            throw new ShowtimeValidationException("showtime.movie.ended");
        }
    }

    private Integer resolveSearchBranchId(Integer requestedBranchId, User actor) {
        if (isAdmin(actor)) {
            return requestedBranchId;
        }
        Integer branchId = forcedBranchId(actor);
        if (branchId == null) {
            return -1;
        }
        if (requestedBranchId != null && !requestedBranchId.equals(branchId)) {
            throw new ShowtimeValidationException("showtime.branch.scope_denied");
        }
        return branchId;
    }

    private Integer resolveSearchHallId(Integer requestedHallId, Integer branchId, User actor) {
        if (requestedHallId == null) {
            return null;
        }
        Hall hall = loadHall(requestedHallId);
        Integer hallBranchId = hallBranchId(hall);
        if (!isAdmin(actor) && !hallBranchId.equals(branchId)) {
            throw new ShowtimeValidationException("showtime.hall.scope_denied");
        }
        if (branchId != null && !hallBranchId.equals(branchId)) {
            throw new ShowtimeValidationException("showtime.hall.branch_mismatch");
        }
        return requestedHallId;
    }

    private Integer resolveTargetBranchId(Integer requestedBranchId, Hall hall, User actor) {
        Integer hallBranchId = hallBranchId(hall);
        if (hallBranchId == null) {
            throw new ShowtimeValidationException("showtime.hall.required");
        }
        if (isAdmin(actor)) {
            if (requestedBranchId != null && !requestedBranchId.equals(hallBranchId)) {
                throw new ShowtimeValidationException("showtime.hall.branch_mismatch");
            }
            return hallBranchId;
        }
        Integer scopedBranchId = forcedBranchId(actor);
        if (scopedBranchId == null) {
            throw new ShowtimeValidationException("showtime.branch.scope_required");
        }
        if (!scopedBranchId.equals(hallBranchId)) {
            throw new ShowtimeValidationException("showtime.hall.scope_denied");
        }
        if (requestedBranchId != null && !requestedBranchId.equals(scopedBranchId)) {
            throw new ShowtimeValidationException("showtime.branch.scope_denied");
        }
        return scopedBranchId;
    }

    private void validateMovieAssignment(Integer movieId, Integer branchId) {
        if (branchMovieRepository.existsByMovie_MovieId(movieId)
                && !branchMovieRepository.existsByIdBranchIdAndIdMovieId(branchId, movieId)) {
            throw new ShowtimeValidationException("showtime.movie.branch_unavailable");
        }
    }

    private void ensureEditable(Integer showtimeId, Showtime showtime) {
        if (showtime.getStartTime() != null && showtime.getStartTime().isBefore(LocalDateTime.now())) {
            throw new ShowtimeValidationException("showtime.cannot_edit_past");
        }
        ensureNoDependencies(showtimeId);
    }

    private void ensureNoDependencies(Integer showtimeId) {
        if (bookingRepository.countByShowtime_ShowtimeId(showtimeId) > 0
                || ticketRepository.countByBooking_Showtime_ShowtimeId(showtimeId) > 0
                || seatReservationRepository.countByShowtime_ShowtimeId(showtimeId) > 0
                || paymentRepository.countByBooking_Showtime_ShowtimeId(showtimeId) > 0) {
            throw new ShowtimeValidationException("showtime.cannot_change_with_dependencies");
        }
    }

    private void assertCanManageBranch(User actor, Integer branchId) {
        if (isAdmin(actor)) {
            return;
        }
        Integer scopedBranchId = forcedBranchId(actor);
        if (scopedBranchId == null || !scopedBranchId.equals(branchId)) {
            throw new ShowtimeValidationException("showtime.branch.scope_denied");
        }
    }

    private Integer forcedBranchId(User actor) {
        if (isManager(actor) || isBranchManager(actor)) {
            Branch branch = actor.getBranch();
            return branch == null ? null : branch.getBranchId();
        }
        if (isAdmin(actor)) {
            return null;
        }
        throw new ShowtimeValidationException("showtime.access.denied");
    }

    private ShowtimeListItem toListItem(Showtime showtime) {
        ShowtimeDetailView detail = toDetailView(showtime);
        return ShowtimeListItem.builder()
                .showtimeId(detail.getShowtimeId())
                .movieId(detail.getMovieId())
                .movieTitle(detail.getMovieTitle())
                .branchId(detail.getBranchId())
                .branchName(detail.getBranchName())
                .hallId(detail.getHallId())
                .hallName(detail.getHallName())
                .startTime(detail.getStartTime())
                .endTime(detail.getEndTime())
                .price(detail.getPrice())
                .priceRange(detail.getPriceRange())
                .seatTypePrices(detail.getSeatTypePrices())
                .bookingCount(detail.getBookingCount())
                .ticketCount(detail.getTicketCount())
                .reservationCount(detail.getReservationCount())
                .past(detail.isPast())
                .canDelete(detail.isCanDelete())
                .canEditSchedule(detail.isCanEditSchedule())
                .build();
    }

    private ShowtimeDetailView toDetailView(Showtime showtime) {
        Integer showtimeId = showtime.getShowtimeId();
        Movie movie = showtime.getMovie();
        Hall hall = showtime.getHall();
        Branch branch = hall == null ? null : hall.getBranch();
        long bookingCount = bookingRepository.countByShowtime_ShowtimeId(showtimeId);
        long ticketCount = ticketRepository.countByBooking_Showtime_ShowtimeId(showtimeId);
        long reservationCount = seatReservationRepository.countByShowtime_ShowtimeId(showtimeId);
        long paymentCount = paymentRepository.countByBooking_Showtime_ShowtimeId(showtimeId);
        boolean past = showtime.getStartTime() != null && showtime.getStartTime().isBefore(LocalDateTime.now());
        boolean cancelled = SHOWTIME_CANCELLED.equals(showtime.getStatus());
        boolean hasDependencies = bookingCount > 0 || ticketCount > 0 || reservationCount > 0 || paymentCount > 0;
        List<SeatTypePriceView> seatTypePrices = hall == null
                ? List.of()
                : hallSeatTypePricingService.priceTable(hall.getHallId(), hall.getTicketPrice());
        return ShowtimeDetailView.builder()
                .showtimeId(showtimeId)
                .movieId(movie == null ? null : movie.getMovieId())
                .movieTitle(movie == null ? "" : movie.getTitle())
                .branchId(branch == null ? null : branch.getBranchId())
                .branchName(branch == null ? "" : branch.getName())
                .city(branch == null ? "" : branch.getCity())
                .hallId(hall == null ? null : hall.getHallId())
                .hallName(hall == null ? "" : hall.getName())
                .startTime(showtime.getStartTime())
                .endTime(showtime.getEndTime())
                .price(ticketPriceFor(hall))
                .priceRange(priceRange(seatTypePrices))
                .seatTypePrices(seatTypePrices)
                .bookingCount(bookingCount)
                .ticketCount(ticketCount)
                .reservationCount(reservationCount)
                .paymentCount(paymentCount)
                .past(past)
                .canDelete(!cancelled && !hasDependencies)
                .canEditSchedule(!cancelled && !hasDependencies && !past)
                .build();
    }

    private BranchOption toBranchOption(Branch branch) {
        return BranchOption.builder()
                .branchId(branch.getBranchId())
                .name(branch.getName())
                .city(branch.getCity())
                .build();
    }

    private HallListItem toHallOption(Hall hall) {
        Branch branch = hall.getBranch();
        return HallListItem.builder()
                .hallId(hall.getHallId())
                .name(hall.getName())
                .branchId(branch == null ? null : branch.getBranchId())
                .branchName(branch == null ? "" : branch.getName())
                .city(branch == null ? "" : branch.getCity())
                .hallType(hall.getHallType())
                .ticketPrice(hall.getTicketPrice())
                .priceRange(priceRange(hallSeatTypePricingService.priceTable(hall.getHallId(), hall.getTicketPrice())))
                .capacity(hall.getCapacity())
                .status(hall.getStatus())
                .build();
    }

    private MovieOption toMovieOption(Movie movie) {
        return MovieOption.builder()
                .movieId(movie.getMovieId())
                .title(movie.getTitle())
                .duration(movie.getDuration())
                .status(movie.getStatus())
                .build();
    }

    private static Integer showtimeBranchId(Showtime showtime) {
        return hallBranchId(showtime.getHall());
    }

    private static Integer hallBranchId(Hall hall) {
        Branch branch = hall == null ? null : hall.getBranch();
        return branch == null ? null : branch.getBranchId();
    }

    private static Integer ticketPriceFor(Hall hall) {
        Integer ticketPrice = hall == null ? null : hall.getTicketPrice();
        if (ticketPrice == null || ticketPrice < 1) {
            throw new ShowtimeValidationException("showtime.hall.ticket_price.invalid");
        }
        return ticketPrice;
    }

    private static String priceRange(List<SeatTypePriceView> prices) {
        List<Integer> positivePrices = prices.stream()
                .map(SeatTypePriceView::getPrice)
                .filter(price -> price != null && price > 0)
                .sorted()
                .toList();
        if (positivePrices.isEmpty()) {
            return "0";
        }
        Integer min = positivePrices.get(0);
        Integer max = positivePrices.get(positivePrices.size() - 1);
        return min.equals(max) ? String.valueOf(min) : min + " - " + max;
    }

    private static String normalizeOptional(String value) {
        return value == null ? null : value.trim();
    }

    private static boolean isAdmin(User user) {
        return isRole(user, "Admin") || isRole(user, "Administrator");
    }

    private static boolean isManager(User user) {
        return isRole(user, "Manager");
    }

    private static boolean isBranchManager(User user) {
        return isRole(user, "BranchManager") || isRole(user, "Branch Manager") || isRole(user, "Branch_Manager");
    }

    private static boolean isRole(User user, String expected) {
        Role role = user.getRole();
        return role != null && normalizeRoleName(expected).equals(normalizeRoleName(role.getName()));
    }

    private static String normalizeRoleName(String roleName) {
        if (roleName == null) {
            return "";
        }
        String normalized = roleName.trim();
        if (normalized.regionMatches(true, 0, "ROLE_", 0, 5)) {
            normalized = normalized.substring(5);
        }
        return normalized.replaceAll("[\\s_]+", "").toUpperCase();
    }

    private static String sidebarFor(User user) {
        if (isAdmin(user)) {
            return "admin";
        }
        if (isManager(user)) {
            return "manager";
        }
        return "branch";
    }
}
