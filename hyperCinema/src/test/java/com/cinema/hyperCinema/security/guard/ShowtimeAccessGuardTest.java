package com.cinema.hyperCinema.security.guard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.Authentication;

import com.cinema.hyperCinema.model.Branch;
import com.cinema.hyperCinema.model.Hall;
import com.cinema.hyperCinema.model.Movie;
import com.cinema.hyperCinema.model.Role;
import com.cinema.hyperCinema.model.Showtime;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.repository.HallRepository;
import com.cinema.hyperCinema.repository.ShowtimeRepository;
import com.cinema.hyperCinema.repository.UserRepository;

class ShowtimeAccessGuardTest {

    private static final Integer BRANCH_ID = 10;
    private static final Integer OTHER_BRANCH_ID = 20;
    private static final Integer HALL_ID = 100;
    private static final Integer SHOWTIME_ID = 200;

    private UserRepository userRepository;
    private HallRepository hallRepository;
    private ShowtimeRepository showtimeRepository;
    private ShowtimeAccessGuard guard;
    private Branch branch;
    private Branch otherBranch;
    private Hall hall;
    private Showtime showtime;

    @BeforeEach
    void setUp() {
        userRepository = org.mockito.Mockito.mock(UserRepository.class);
        hallRepository = org.mockito.Mockito.mock(HallRepository.class);
        showtimeRepository = org.mockito.Mockito.mock(ShowtimeRepository.class);
        guard = new ShowtimeAccessGuard(userRepository, hallRepository, showtimeRepository);

        branch = branch(BRANCH_ID);
        otherBranch = branch(OTHER_BRANCH_ID);
        hall = hall(HALL_ID, branch);
        showtime = new Showtime();
        showtime.setShowtimeId(SHOWTIME_ID);
        showtime.setHall(hall);
        showtime.setMovie(new Movie());

        when(hallRepository.findById(HALL_ID)).thenReturn(Optional.of(hall));
        when(showtimeRepository.findByIdWithMovieHallAndBranch(SHOWTIME_ID)).thenReturn(Optional.of(showtime));
    }

    @Test
    void canManageShowtime_whenAdmin_returnsTrue() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user("admin", "Admin", null)));

        assertThat(guard.canManageShowtime(auth("admin"), SHOWTIME_ID)).isTrue();
    }

    @Test
    void canManageShowtime_whenManagerOwnsBranch_returnsTrue() {
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(user("manager", "Manager", branch)));

        assertThat(guard.canManageShowtime(auth("manager"), SHOWTIME_ID)).isTrue();
    }

    @Test
    void canManageShowtime_whenBranchManagerOwnsBranch_returnsTrue() {
        when(userRepository.findByUsername("branch")).thenReturn(Optional.of(user("branch", "BranchManager", branch)));

        assertThat(guard.canManageShowtime(auth("branch"), SHOWTIME_ID)).isTrue();
    }

    @Test
    void canManageShowtime_whenManagerHasWrongBranch_returnsFalse() {
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(user("manager", "Manager", otherBranch)));

        assertThat(guard.canManageShowtime(auth("manager"), SHOWTIME_ID)).isFalse();
    }

    @Test
    void canManageShowtime_whenCustomer_returnsFalse() {
        when(userRepository.findByUsername("customer")).thenReturn(Optional.of(user("customer", "Customer", branch)));

        assertThat(guard.canManageShowtime(auth("customer"), SHOWTIME_ID)).isFalse();
    }

    @Test
    void canManageHall_whenStaff_returnsFalse() {
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(user("staff", "Staff", branch)));

        assertThat(guard.canManageHall(auth("staff"), HALL_ID)).isFalse();
    }

    private static Authentication auth(String username) {
        return new UsernamePasswordAuthenticationToken(
                username,
                "n/a",
                java.util.List.of(new SimpleGrantedAuthority("ROLE_TEST")));
    }

    private static Branch branch(Integer id) {
        Branch branch = new Branch();
        branch.setBranchId(id);
        branch.setName("Branch " + id);
        return branch;
    }

    private static Hall hall(Integer id, Branch branch) {
        Hall hall = new Hall();
        hall.setHallId(id);
        hall.setName("Hall " + id);
        hall.setBranch(branch);
        return hall;
    }

    private static User user(String username, String roleName, Branch branch) {
        Role role = new Role();
        role.setName(roleName);

        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.test");
        user.setRole(role);
        user.setBranch(branch);
        return user;
    }
}
