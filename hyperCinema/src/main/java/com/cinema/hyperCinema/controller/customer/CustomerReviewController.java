package com.cinema.hyperCinema.controller.customer;

import com.cinema.hyperCinema.model.Booking;
import com.cinema.hyperCinema.model.Movie;
import com.cinema.hyperCinema.model.Review;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.repository.BookingRepository;
import com.cinema.hyperCinema.repository.MovieRepository;
import com.cinema.hyperCinema.repository.ReviewRepository;
import com.cinema.hyperCinema.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/my/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerReviewController {

    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final MovieRepository movieRepository;

    @GetMapping
    public String reviewHistory(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "filter", required = false, defaultValue = "all") String filter,
            @RequestParam(name = "sort", required = false, defaultValue = "newest") String sort,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "ajax", required = false, defaultValue = "false") boolean ajax,
            @AuthenticationPrincipal CustomUserDetails userDetails, 
            Model model) {
        User user = userDetails.getUser();
        List<Booking> bookings = bookingRepository.findSuccessfulBookingsByUser(user.getUserId());
        LocalDateTime now = LocalDateTime.now();
        bookings = bookings.stream()
                .filter(booking -> booking.getShowtime() != null
                        && booking.getShowtime().getEndTime() != null
                        && booking.getShowtime().getEndTime().isBefore(now))
                .collect(Collectors.toList());
        List<Review> reviews = reviewRepository.findByUser_UserId(user.getUserId());

        // Create a map of Movie ID -> Review to easily lookup in thymeleaf
        Map<Integer, Review> movieReviewMap = reviews.stream()
                .collect(Collectors.toMap(
                        r -> r.getMovie().getMovieId(),
                        r -> r,
                        (r1, r2) -> r1
                ));

        // 1. Search Filter (by Movie Title)
        if (search != null && !search.trim().isEmpty()) {
            String searchLower = search.toLowerCase().trim();
            bookings = bookings.stream()
                    .filter(b -> b.getShowtime().getMovie().getTitle().toLowerCase().contains(searchLower))
                    .collect(Collectors.toList());
        }

        // 2. Review Status Filter
        if ("reviewed".equals(filter)) {
            bookings = bookings.stream()
                    .filter(b -> movieReviewMap.containsKey(b.getShowtime().getMovie().getMovieId()))
                    .collect(Collectors.toList());
        } else if ("pending".equals(filter)) {
            bookings = bookings.stream()
                    .filter(b -> !movieReviewMap.containsKey(b.getShowtime().getMovie().getMovieId()))
                    .collect(Collectors.toList());
        }

        // 3. Sorting
        if ("oldest".equals(sort)) {
            bookings.sort((b1, b2) -> b1.getCreatedAt().compareTo(b2.getCreatedAt()));
        } else if ("showtime".equals(sort)) {
            bookings.sort((b1, b2) -> b1.getShowtime().getStartTime().compareTo(b2.getShowtime().getStartTime()));
        } else { // newest (default)
            bookings.sort((b1, b2) -> b2.getCreatedAt().compareTo(b1.getCreatedAt()));
        }

        // 4. Pagination (5 items per page)
        int pageSize = 5;
        int totalElements = bookings.size();
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        int currentPage = totalPages == 0 ? 0 : Math.max(0, Math.min(page, totalPages - 1));
        int fromIndex = currentPage * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, totalElements);
        List<Booking> paginatedBookings = (fromIndex < totalElements) ? bookings.subList(fromIndex, toIndex) : Collections.emptyList();
        int paginationStart = Math.max(0, currentPage - 2);
        int paginationEnd = Math.min(totalPages - 1, paginationStart + 4);
        paginationStart = Math.max(0, paginationEnd - 4);

        model.addAttribute("customerName", user.getFullName());
        model.addAttribute("bookings", paginatedBookings);
        model.addAttribute("movieReviewMap", movieReviewMap);
        model.addAttribute("now", now);
        model.addAttribute("active", "reviews");
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("paginationStart", paginationStart);
        model.addAttribute("paginationEnd", paginationEnd);
        model.addAttribute("totalReviewsCount", totalElements);
        model.addAttribute("search", search);
        model.addAttribute("filter", filter);
        model.addAttribute("sort", sort);
        model.addAttribute("lastUpdated",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));

        if (ajax) {
            return "my/reviews :: tickets-list-fragment";
        }
        return "my/reviews";
    }

    @PostMapping
    public String addReview(
            @RequestParam Integer movieId,
            @RequestParam Integer rating,
            @RequestParam String comment,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        User user = userDetails.getUser();

        if (rating == null || rating < 1 || rating > 5) {
            redirectAttributes.addFlashAttribute("errorKey", "review.rating.required");
            return "redirect:/my/reviews";
        }

        if (comment == null || comment.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorKey", "review.comment.required");
            return "redirect:/my/reviews";
        }

        // Verify that this customer has purchased a ticket and the showtime has ended
        boolean canReview = bookingRepository.hasEndedSuccessfulBookingForMovie(
                user.getUserId(),
                movieId,
                LocalDateTime.now());

        if (!canReview) {
            redirectAttributes.addFlashAttribute("errorKey", "review.no_ended_booking");
            return "redirect:/my/reviews";
        }

        // Check if already reviewed
        boolean alreadyReviewed = reviewRepository.findByUser_UserIdAndMovie_MovieId(user.getUserId(), movieId).isPresent();
        if (alreadyReviewed) {
            redirectAttributes.addFlashAttribute("errorKey", "review.already_reviewed");
            return "redirect:/my/reviews";
        }

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phim ID: " + movieId));

        Review review = new Review();
        review.setUser(user);
        review.setMovie(movie);
        review.setRating(rating);
        review.setComment(comment.trim());

        reviewRepository.save(review);

        redirectAttributes.addFlashAttribute("successKey", "review.create.success");
        return "redirect:/my/reviews";
    }
}
