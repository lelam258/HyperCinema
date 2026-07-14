package com.cinema.hyperCinema.controller;

import com.cinema.hyperCinema.dto.admin.movie.request.MovieSearchCriteria;
import com.cinema.hyperCinema.dto.admin.movie.response.MovieDetailView;
import com.cinema.hyperCinema.dto.admin.movie.response.MovieListItem;
import com.cinema.hyperCinema.dto.admin.seat.response.ShowtimeSeatView;
import com.cinema.hyperCinema.model.Movie;
import com.cinema.hyperCinema.model.Review;
import com.cinema.hyperCinema.model.Showtime;
import com.cinema.hyperCinema.repository.GenreRepository;
import com.cinema.hyperCinema.repository.LanguageRepository;
import com.cinema.hyperCinema.repository.MovieRepository;
import com.cinema.hyperCinema.repository.ReviewRepository;
import com.cinema.hyperCinema.repository.ShowtimeRepository;
import com.cinema.hyperCinema.security.CustomUserDetails;
import com.cinema.hyperCinema.service.movie.MovieService;
import com.cinema.hyperCinema.service.seat.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/movies")
@RequiredArgsConstructor
public class MovieCustomerController {

    private final MovieService movieService;
    private final LanguageRepository languageRepository;
    private final GenreRepository genreRepository;
    private final SeatService seatService;
    private final ShowtimeRepository showtimeRepository;
    private final ReviewRepository reviewRepository;
    private final MovieRepository movieRepository;

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
    public String detail(@PathVariable Integer movieId, Model model, Authentication authentication) {
        MovieDetailView movie = movieService.findById(movieId);
        model.addAttribute("movie", movie);

        List<Review> reviews = reviewRepository.findByMovieIdWithUser(movieId);
        model.addAttribute("reviews", reviews);

        double averageRating = 0.0;
        if (!reviews.isEmpty()) {
            averageRating = reviews.stream().mapToDouble(Review::getRating).average().orElse(0.0);
            averageRating = Math.round(averageRating * 10.0) / 10.0;
        }
        model.addAttribute("averageRating", averageRating);
        model.addAttribute("reviewCount", reviews.size());

        boolean isLoggedInCustomer = false;
        if (authentication != null && authentication.isAuthenticated()) {
            isLoggedInCustomer = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"));
        }
        model.addAttribute("isLoggedInCustomer", isLoggedInCustomer);

        return "customer/movies/detail";
    }

    @PostMapping("/{movieId}/reviews")
    @PreAuthorize("hasRole('CUSTOMER')")
    public String addReview(
            @PathVariable Integer movieId,
            @RequestParam Integer rating,
            @RequestParam String comment,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        if (rating == null || rating < 1 || rating > 5) {
            redirectAttributes.addFlashAttribute("errorKey", "review.rating.required");
            return "redirect:/movies/" + movieId;
        }

        if (comment == null || comment.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorKey", "review.comment.required");
            return "redirect:/movies/" + movieId;
        }

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phim ID: " + movieId));

        Review review = new Review();
        review.setUser(userDetails.getUser());
        review.setMovie(movie);
        review.setRating(rating);
        review.setComment(comment.trim());

        reviewRepository.save(review);

        redirectAttributes.addFlashAttribute("successKey", "review.create.success");
        return "redirect:/movies/" + movieId;
    }


    @GetMapping("/showtimes/{showtimeId}/seats")
    public String viewShowtimeSeats(@PathVariable Integer showtimeId, Model model, Authentication authentication) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy suất chiếu ID: " + showtimeId));

        List<ShowtimeSeatView> seats = seatService.getSeatsForShowtime(showtimeId);

        boolean isLoggedInCustomer = false;
        if (authentication != null && authentication.isAuthenticated()) {
            isLoggedInCustomer = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"));
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
