package com.cinema.hyperCinema.service.showtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.cinema.hyperCinema.dto.admin.showtime.request.ShowtimeCreateRequest;
import com.cinema.hyperCinema.dto.admin.showtime.request.ShowtimeUpdateRequest;
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
import com.cinema.hyperCinema.service.showtime.impl.ShowtimeServiceImpl;

class ShowtimeServiceImplTest {

    private static final Integer ADMIN_ID = 1;
    private static final Integer MANAGER_ID = 2;
    private static final Integer BRANCH_ID = 10;
    private static final Integer OTHER_BRANCH_ID = 20;
    private static final Integer HALL_ID = 100;
    private static final Integer OTHER_HALL_ID = 200;
    private static final Integer MOVIE_ID = 300;
    private static final Integer SHOWTIME_ID = 400;
    private static final LocalDateTime START = LocalDateTime.now().plusDays(2).withSecond(0).withNano(0);
    private static final LocalDateTime END = START.plusHours(2);

    private ShowtimeRepository showtimeRepository;
    private MovieRepository movieRepository;
    private BranchRepository branchRepository;
    private HallRepository hallRepository;
    private BranchMovieRepository branchMovieRepository;
    private BookingRepository bookingRepository;
    private TicketRepository ticketRepository;
    private SeatReservationRepository seatReservationRepository;
    private PaymentRepository paymentRepository;
    private UserRepository userRepository;
    private ShowtimeServiceImpl service;
    private User adminActor;
    private User managerActor;
    private Movie movie;
    private Hall hall;
    private Hall otherHall;

    @BeforeEach
    void setUp() {
        showtimeRepository = org.mockito.Mockito.mock(ShowtimeRepository.class);
        movieRepository = org.mockito.Mockito.mock(MovieRepository.class);
        branchRepository = org.mockito.Mockito.mock(BranchRepository.class);
        hallRepository = org.mockito.Mockito.mock(HallRepository.class);
        branchMovieRepository = org.mockito.Mockito.mock(BranchMovieRepository.class);
        bookingRepository = org.mockito.Mockito.mock(BookingRepository.class);
        ticketRepository = org.mockito.Mockito.mock(TicketRepository.class);
        seatReservationRepository = org.mockito.Mockito.mock(SeatReservationRepository.class);
        paymentRepository = org.mockito.Mockito.mock(PaymentRepository.class);
        userRepository = org.mockito.Mockito.mock(UserRepository.class);

        Branch branch = branch(BRANCH_ID, "Main");
        Branch otherBranch = branch(OTHER_BRANCH_ID, "Other");
        hall = hall(HALL_ID, "A1", branch);
        otherHall = hall(OTHER_HALL_ID, "B1", otherBranch);
        movie = movie(MOVIE_ID, "Interstellar");
        adminActor = user(ADMIN_ID, "Admin", null);
        managerActor = user(MANAGER_ID, "Manager", branch);

        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(adminActor));
        when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(managerActor));
        when(movieRepository.findById(MOVIE_ID)).thenReturn(Optional.of(movie));
        when(hallRepository.findById(HALL_ID)).thenReturn(Optional.of(hall));
        when(hallRepository.findById(OTHER_HALL_ID)).thenReturn(Optional.of(otherHall));
        when(branchMovieRepository.existsByMovie_MovieId(MOVIE_ID)).thenReturn(false);
        when(showtimeRepository.save(any(Showtime.class))).thenAnswer(inv -> {
            Showtime saved = inv.getArgument(0);
            saved.setShowtimeId(SHOWTIME_ID);
            return saved;
        });

        service = new ShowtimeServiceImpl(showtimeRepository, movieRepository, branchRepository,
                hallRepository, branchMovieRepository, bookingRepository, ticketRepository,
                seatReservationRepository, paymentRepository, userRepository);
    }

    @Test
    void create_whenAdminSubmitsValidShowtime_savesSchedule() {
        ShowtimeCreateRequest request = validCreateRequest();

        service.create(request, adminActor);

        ArgumentCaptor<Showtime> captor = ArgumentCaptor.forClass(Showtime.class);
        verify(showtimeRepository).save(captor.capture());
        Showtime saved = captor.getValue();
        assertThat(saved.getMovie()).isSameAs(movie);
        assertThat(saved.getHall()).isSameAs(hall);
        assertThat(saved.getStartTime()).isEqualTo(START);
        assertThat(saved.getEndTime()).isEqualTo(END);
        assertThat(saved.getPrice()).isEqualTo(120000);
    }

    @Test
    void create_whenSameHallOverlaps_rejectsSchedule() {
        when(showtimeRepository.existsOverlap(HALL_ID, START, END)).thenReturn(true);

        assertThatThrownBy(() -> service.create(validCreateRequest(), adminActor))
                .isInstanceOf(ShowtimeValidationException.class)
                .hasMessage("showtime.time.overlap");

        verify(showtimeRepository, never()).save(any(Showtime.class));
    }

    @Test
    void create_whenManagerUsesHallOutsideAssignedBranch_rejectsSchedule() {
        ShowtimeCreateRequest request = validCreateRequest();
        request.setHallId(OTHER_HALL_ID);

        assertThatThrownBy(() -> service.create(request, managerActor))
                .isInstanceOf(ShowtimeValidationException.class)
                .hasMessage("showtime.hall.scope_denied");

        verify(showtimeRepository, never()).save(any(Showtime.class));
    }

    @Test
    void create_whenMovieAssignedToDifferentBranch_rejectsSchedule() {
        when(branchMovieRepository.existsByMovie_MovieId(MOVIE_ID)).thenReturn(true);
        when(branchMovieRepository.existsByIdBranchIdAndIdMovieId(BRANCH_ID, MOVIE_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.create(validCreateRequest(), adminActor))
                .isInstanceOf(ShowtimeValidationException.class)
                .hasMessage("showtime.movie.branch_unavailable");
    }

    @Test
    void update_whenShowtimeHasBookings_rejectsUnsafeChange() {
        Showtime showtime = showtime(SHOWTIME_ID, hall, movie, START, END);
        when(showtimeRepository.findByIdWithMovieHallAndBranch(SHOWTIME_ID)).thenReturn(Optional.of(showtime));
        when(bookingRepository.countByShowtime_ShowtimeId(SHOWTIME_ID)).thenReturn(1L);

        assertThatThrownBy(() -> service.update(SHOWTIME_ID, validUpdateRequest(), adminActor))
                .isInstanceOf(ShowtimeValidationException.class)
                .hasMessage("showtime.cannot_change_with_dependencies");

        verify(showtimeRepository, never()).save(any(Showtime.class));
    }

    @Test
    void delete_whenShowtimeHasDependencies_rejectsDelete() {
        Showtime showtime = showtime(SHOWTIME_ID, hall, movie, START, END);
        when(showtimeRepository.findByIdWithMovieHallAndBranch(SHOWTIME_ID)).thenReturn(Optional.of(showtime));
        when(ticketRepository.countByBooking_Showtime_ShowtimeId(SHOWTIME_ID)).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(SHOWTIME_ID, adminActor))
                .isInstanceOf(ShowtimeValidationException.class)
                .hasMessage("showtime.cannot_change_with_dependencies");

        verify(showtimeRepository, never()).delete(any(Showtime.class));
    }

    private ShowtimeCreateRequest validCreateRequest() {
        return new ShowtimeCreateRequest(MOVIE_ID, BRANCH_ID, HALL_ID, START, END, 120000);
    }

    private ShowtimeUpdateRequest validUpdateRequest() {
        ShowtimeUpdateRequest request = new ShowtimeUpdateRequest();
        request.setMovieId(MOVIE_ID);
        request.setBranchId(BRANCH_ID);
        request.setHallId(HALL_ID);
        request.setStartTime(START.plusDays(1));
        request.setEndTime(END.plusDays(1));
        request.setPrice(130000);
        return request;
    }

    private static Branch branch(Integer id, String name) {
        Branch branch = new Branch();
        branch.setBranchId(id);
        branch.setName(name);
        branch.setCity("HCM");
        return branch;
    }

    private static Hall hall(Integer id, String name, Branch branch) {
        Hall hall = new Hall();
        hall.setHallId(id);
        hall.setName(name);
        hall.setBranch(branch);
        hall.setHallType("2D");
        hall.setCapacity(100);
        hall.setStatus("Active");
        return hall;
    }

    private static Movie movie(Integer id, String title) {
        Movie movie = new Movie();
        movie.setMovieId(id);
        movie.setTitle(title);
        movie.setDuration(120);
        movie.setDescription("Test");
        movie.setReleaseDate(LocalDate.now());
        movie.setStatus("NowShowing");
        movie.setLanguageId(1);
        return movie;
    }

    private static Showtime showtime(Integer id, Hall hall, Movie movie,
                                     LocalDateTime startTime, LocalDateTime endTime) {
        Showtime showtime = new Showtime();
        showtime.setShowtimeId(id);
        showtime.setHall(hall);
        showtime.setMovie(movie);
        showtime.setStartTime(startTime);
        showtime.setEndTime(endTime);
        showtime.setPrice(120000);
        return showtime;
    }

    private static User user(Integer id, String roleName, Branch branch) {
        Role role = new Role();
        role.setRoleId(id);
        role.setName(roleName);

        User user = new User();
        user.setUserId(id);
        user.setUsername("user" + id);
        user.setEmail("user" + id + "@example.test");
        user.setRole(role);
        user.setBranch(branch);
        return user;
    }
}
