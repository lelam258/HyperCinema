package com.cinema.hyperCinema.controller;

import com.cinema.hyperCinema.dto.admin.movie.request.MovieSearchCriteria;
import com.cinema.hyperCinema.dto.admin.movie.response.MovieDetailView;
import com.cinema.hyperCinema.dto.admin.movie.response.MovieListItem;
import com.cinema.hyperCinema.dto.admin.seat.response.ShowtimeSeatView;
import com.cinema.hyperCinema.model.Showtime;
import com.cinema.hyperCinema.repository.GenreRepository;
import com.cinema.hyperCinema.repository.LanguageRepository;
import com.cinema.hyperCinema.repository.ShowtimeRepository;
import com.cinema.hyperCinema.security.CustomUserDetails;
import com.cinema.hyperCinema.service.movie.MovieService;
import com.cinema.hyperCinema.service.seat.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/movies")
@RequiredArgsConstructor
public class MovieCustomerController {

    private static final String SHOWTIME_CANCELLED = "CANCELLED";

    private final MovieService movieService;
    private final LanguageRepository languageRepository;
    private final GenreRepository genreRepository;
    private final SeatService seatService;
    private final ShowtimeRepository showtimeRepository;

    @GetMapping
    public String list(@ModelAttribute("criteria") MovieSearchCriteria criteria, Model model) {
        criteria.normalize();

        // For public customer view, default to showing NowShowing movies if no status is specified
        if (criteria.getStatus() == null || criteria.getStatus().isBlank()) {
            criteria.setStatus("NowShowing");
        }

        Sort.Direction direction = Sort.Direction.fromString(criteria.getDirection());
        String sortField = mapSortField(criteria.getSort());
        Pageable pageable = PageRequest.of(
                criteria.getPage(),
                criteria.getSize(),
                Sort.by(direction, sortField));

        Page<MovieListItem> page = movieService.search(criteria, pageable);
        model.addAttribute("page", page);
        model.addAttribute("criteria", criteria);
        
        model.addAttribute("languages", languageRepository.findAll(Sort.by("name")));
        model.addAttribute("genres", genreRepository.findAllByOrderByNameAsc());

        return "customer/movies/list";
    }

    @GetMapping("/{movieId}")
    public String detail(@PathVariable Integer movieId, Model model) {
        MovieDetailView movie = movieService.findById(movieId);
        model.addAttribute("movie", movie);
        return "customer/movies/detail";
    }

    @GetMapping("/showtimes/{showtimeId}/seats")
    public String viewShowtimeSeats(@PathVariable Integer showtimeId, Model model, Authentication authentication) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy suất chiếu ID: " + showtimeId));

        if (SHOWTIME_CANCELLED.equals(showtime.getStatus())) {
            throw new IllegalArgumentException("Không tìm thấy suất chiếu ID: " + showtimeId);
        }

        List<ShowtimeSeatView> seats = seatService.getSeatsForShowtime(showtimeId);

        boolean isLoggedInCustomer = false;
        if (authentication != null && authentication.isAuthenticated()) {
            isLoggedInCustomer = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"));
            if (isLoggedInCustomer && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
                model.addAttribute("customerName", userDetails.getUser().getFullName());
            }
        }

        model.addAttribute("showtime", showtime);
        model.addAttribute("seats", seats);
        model.addAttribute("isLoggedInCustomer", isLoggedInCustomer);

        return "customer/movies/seat-view";
    }

    private String mapSortField(String sort) {
        return switch (sort) {
            case "release_date", "releaseDate" -> "releaseDate";
            case "created_at", "createdAt" -> "createdAt";
            default -> sort;
        };
    }
}
