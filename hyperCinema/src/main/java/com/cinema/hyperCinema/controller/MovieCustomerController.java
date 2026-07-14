package com.cinema.hyperCinema.controller;

import com.cinema.hyperCinema.dto.admin.movie.request.MovieSearchCriteria;
import com.cinema.hyperCinema.dto.admin.movie.response.MovieDetailView;
import com.cinema.hyperCinema.dto.admin.movie.response.MovieListItem;
import com.cinema.hyperCinema.dto.admin.seat.response.ShowtimeSeatView;
import com.cinema.hyperCinema.model.Movie;
import com.cinema.hyperCinema.model.Review;
import com.cinema.hyperCinema.model.Showtime;
import com.cinema.hyperCinema.model.ReviewReply;
import com.cinema.hyperCinema.model.ReviewInteraction;
import com.cinema.hyperCinema.repository.BookingRepository;
import com.cinema.hyperCinema.repository.GenreRepository;
import com.cinema.hyperCinema.repository.LanguageRepository;
import com.cinema.hyperCinema.repository.MovieRepository;
import com.cinema.hyperCinema.repository.ReviewInteractionRepository;
import com.cinema.hyperCinema.repository.ReviewReplyRepository;
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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
    private final BookingRepository bookingRepository;
    private final ReviewReplyRepository reviewReplyRepository;
    private final ReviewInteractionRepository reviewInteractionRepository;



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
    public String detail(
            @PathVariable Integer movieId,
            @RequestParam(name = "sort", required = false, defaultValue = "newest") String sort,
            @RequestParam(name = "rating", required = false) Integer ratingFilter,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "ajax", required = false, defaultValue = "false") boolean ajax,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {
        MovieDetailView movie = movieService.findById(movieId);
        model.addAttribute("movie", movie);

        java.util.Set<Integer> likedReviewIds = new java.util.HashSet<>();
        java.util.Set<Integer> dislikedReviewIds = new java.util.HashSet<>();
        if (userDetails != null) {
            List<ReviewInteraction> interactions = reviewInteractionRepository.findByUser_UserId(userDetails.getUser().getUserId());
            for (ReviewInteraction interaction : interactions) {
                if (interaction.getIsLike()) {
                    likedReviewIds.add(interaction.getReview().getReviewId());
                } else {
                    dislikedReviewIds.add(interaction.getReview().getReviewId());
                }
            }
        }
        model.addAttribute("likedReviewIds", likedReviewIds);
        model.addAttribute("dislikedReviewIds", dislikedReviewIds);

        List<Review> allReviews = reviewRepository.findByMovieIdWithUser(movieId);

        // Compute average rating before filtering for correct stats
        double averageRating = 0.0;
        if (!allReviews.isEmpty()) {
            averageRating = allReviews.stream().mapToDouble(Review::getRating).average().orElse(0.0);
            averageRating = Math.round(averageRating * 10.0) / 10.0;
        }
        model.addAttribute("averageRating", averageRating);
        model.addAttribute("reviewCount", allReviews.size());
        model.addAttribute("reviewsListForStats", allReviews); // Keep list for stats chart calculation

        // Apply filters
        List<Review> filteredReviews = allReviews;
        if (ratingFilter != null) {
            filteredReviews = filteredReviews.stream()
                    .filter(r -> r.getRating().equals(ratingFilter))
                    .collect(Collectors.toList());
        }

        // Apply sorting
        if ("rating_desc".equals(sort)) {
            filteredReviews.sort((r1, r2) -> r2.getRating().compareTo(r1.getRating()));
        } else if ("rating_asc".equals(sort)) {
            filteredReviews.sort((r1, r2) -> r1.getRating().compareTo(r2.getRating()));
        } else if ("likes".equals(sort)) {
            filteredReviews.sort((r1, r2) -> r2.getLikesCount().compareTo(r1.getLikesCount()));
        } else { // newest
            filteredReviews.sort((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt()));
        }

        // Apply pagination (5 items per page)
        int pageSize = 5;
        int totalElements = filteredReviews.size();
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        int fromIndex = page * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, totalElements);
        List<Review> paginatedReviews = (fromIndex < totalElements) ? filteredReviews.subList(fromIndex, toIndex) : Collections.emptyList();

        model.addAttribute("reviews", paginatedReviews);
        model.addAttribute("sort", sort);
        model.addAttribute("ratingFilter", ratingFilter);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalReviewsCount", totalElements);

        if (ajax) {
            return "customer/movies/detail :: reviews-list-fragment";
        }
        return "customer/movies/detail";
    }

    @PostMapping("/reviews/{reviewId}/like")
    @org.springframework.web.bind.annotation.ResponseBody
    @PreAuthorize("hasRole('CUSTOMER')")
    public org.springframework.http.ResponseEntity<?> likeReview(
            @PathVariable Integer reviewId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá ID: " + reviewId));
        com.cinema.hyperCinema.model.User user = userDetails.getUser();
        
        java.util.Optional<ReviewInteraction> existingOpt = reviewInteractionRepository
                .findByReview_ReviewIdAndUser_UserId(reviewId, user.getUserId());
        
        String state = "like";
        if (existingOpt.isPresent()) {
            ReviewInteraction existing = existingOpt.get();
            if (existing.getIsLike()) {
                // Toggle off (unlike)
                reviewInteractionRepository.delete(existing);
                review.setLikesCount(Math.max(0, review.getLikesCount() - 1));
                state = "none";
            } else {
                // Switch from dislike to like
                existing.setIsLike(true);
                reviewInteractionRepository.save(existing);
                review.setLikesCount(review.getLikesCount() + 1);
                review.setDislikesCount(Math.max(0, review.getDislikesCount() - 1));
            }
        } else {
            // New like
            ReviewInteraction interaction = new ReviewInteraction();
            interaction.setReview(review);
            interaction.setUser(user);
            interaction.setIsLike(true);
            reviewInteractionRepository.save(interaction);
            review.setLikesCount(review.getLikesCount() + 1);
        }
        reviewRepository.save(review);
        
        return org.springframework.http.ResponseEntity.ok(java.util.Map.of(
            "likesCount", review.getLikesCount(),
            "dislikesCount", review.getDislikesCount(),
            "userLikeState", state
        ));
    }

    @PostMapping("/reviews/{reviewId}/dislike")
    @org.springframework.web.bind.annotation.ResponseBody
    @PreAuthorize("hasRole('CUSTOMER')")
    public org.springframework.http.ResponseEntity<?> dislikeReview(
            @PathVariable Integer reviewId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá ID: " + reviewId));
        com.cinema.hyperCinema.model.User user = userDetails.getUser();
        
        java.util.Optional<ReviewInteraction> existingOpt = reviewInteractionRepository
                .findByReview_ReviewIdAndUser_UserId(reviewId, user.getUserId());
        
        String state = "dislike";
        if (existingOpt.isPresent()) {
            ReviewInteraction existing = existingOpt.get();
            if (!existing.getIsLike()) {
                // Toggle off (undislike)
                reviewInteractionRepository.delete(existing);
                review.setDislikesCount(Math.max(0, review.getDislikesCount() - 1));
                state = "none";
            } else {
                // Switch from like to dislike
                existing.setIsLike(false);
                reviewInteractionRepository.save(existing);
                review.setDislikesCount(review.getDislikesCount() + 1);
                review.setLikesCount(Math.max(0, review.getLikesCount() - 1));
            }
        } else {
            // New dislike
            ReviewInteraction interaction = new ReviewInteraction();
            interaction.setReview(review);
            interaction.setUser(user);
            interaction.setIsLike(false);
            reviewInteractionRepository.save(interaction);
            review.setDislikesCount(review.getDislikesCount() + 1);
        }
        reviewRepository.save(review);
        
        return org.springframework.http.ResponseEntity.ok(java.util.Map.of(
            "likesCount", review.getLikesCount(),
            "dislikesCount", review.getDislikesCount(),
            "userLikeState", state
        ));
    }

    @PostMapping("/reviews/{reviewId}/replies")
    @org.springframework.web.bind.annotation.ResponseBody
    @PreAuthorize("hasRole('CUSTOMER')")
    public org.springframework.http.ResponseEntity<?> replyToReview(
            @PathVariable Integer reviewId,
            @RequestParam String content,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá ID: " + reviewId));
        
        ReviewReply reply = new ReviewReply();
        reply.setReview(review);
        reply.setUser(userDetails.getUser());
        reply.setContent(content.trim());
        reviewReplyRepository.save(reply);
        
        return org.springframework.http.ResponseEntity.ok().build();
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
