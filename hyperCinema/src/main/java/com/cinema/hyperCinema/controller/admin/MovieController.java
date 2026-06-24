package com.cinema.hyperCinema.controller.admin;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cinema.hyperCinema.dto.admin.movie.request.AddGenreRequest;
import com.cinema.hyperCinema.dto.admin.movie.request.AssignBranchRequest;
import com.cinema.hyperCinema.dto.admin.movie.request.MovieCreateRequest;
import com.cinema.hyperCinema.dto.admin.movie.request.MovieSearchCriteria;
import com.cinema.hyperCinema.dto.admin.movie.request.MovieStatusChangeRequest;
import com.cinema.hyperCinema.dto.admin.movie.request.MovieUpdateRequest;
import com.cinema.hyperCinema.dto.admin.movie.response.GenreSummary;
import com.cinema.hyperCinema.dto.admin.movie.response.MovieDetailView;
import com.cinema.hyperCinema.dto.admin.movie.response.MovieListItem;
import com.cinema.hyperCinema.dto.admin.movie.response.UpdateResult;
import com.cinema.hyperCinema.exception.movie.MovieValidationException;
import com.cinema.hyperCinema.repository.BranchRepository;
import com.cinema.hyperCinema.repository.GenreRepository;
import com.cinema.hyperCinema.repository.LanguageRepository;
import com.cinema.hyperCinema.security.CustomUserDetails;
import com.cinema.hyperCinema.service.media.CloudinaryImageService;
import com.cinema.hyperCinema.service.movie.MovieService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/movies")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class MovieController {

    private static final Set<String> ALLOWED_STATUSES =
            Set.of("ComingSoon", "NowShowing", "Ended");

    private static final String WARNING_KEY_STATUS_IGNORED =
            "movie.search.status_ignored";

    private static final String WARNING_KEY_RANGE_IGNORED =
            "movie.search.release_date_range_ignored";

    private static final String ROLE_ADMIN_AUTHORITY = "ROLE_ADMIN";

    private final MovieService movieService;
    private final LanguageRepository languageRepository;
    private final GenreRepository genreRepository;
    private final BranchRepository branchRepository;
    private final CloudinaryImageService cloudinaryImageService;

    @GetMapping
    public String list(@ModelAttribute("criteria") MovieSearchCriteria criteria,
                       Model model) {

        criteria.normalize();

        String rawStatus = criteria.getStatus();
        if (rawStatus != null && !rawStatus.isBlank()
                && !ALLOWED_STATUSES.contains(rawStatus)) {
            criteria.setStatus(null);
            model.addAttribute("warningKey", WARNING_KEY_STATUS_IGNORED);
        }

        boolean onlyOneSide =
                (criteria.getReleaseDateFrom() == null)
                        ^ (criteria.getReleaseDateTo() == null);
        boolean inverted = criteria.getReleaseDateFrom() != null
                && criteria.getReleaseDateTo() != null
                && criteria.getReleaseDateFrom().isAfter(criteria.getReleaseDateTo());
        if (onlyOneSide || inverted) {
            criteria.setReleaseDateFrom(null);
            criteria.setReleaseDateTo(null);
            if (inverted) {
                model.addAttribute("warningKey", WARNING_KEY_RANGE_IGNORED);
            }
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
        prepareFormDropdowns(model);

        return "admin/movies/movie-list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("movie", new MovieCreateRequest());
        model.addAttribute("mode", "create");
        prepareFormDropdowns(model);
        return "admin/movies/movie-form";
    }

    @PostMapping(value = "/poster-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, String>> uploadPoster(@RequestParam("posterFile") MultipartFile posterFile) {
        try {
            String posterUrl = cloudinaryImageService.uploadMoviePoster(posterFile);
            return ResponseEntity.ok(Map.of("url", posterUrl));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("errorKey", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.internalServerError().body(Map.of("errorKey", "movie.poster_upload_failed"));
        }
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("movie") MovieCreateRequest movie,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal CustomUserDetails principal,
                         RedirectAttributes redirectAttributes,
                         Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", "create");
            prepareFormDropdowns(model);
            return "admin/movies/movie-form";
        }

        uploadPosterIfPresent(movie, bindingResult);
        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", "create");
            prepareFormDropdowns(model);
            return "admin/movies/movie-form";
        }

        try {
            MovieDetailView created = movieService.create(movie, principal.getUser());

            redirectAttributes.addFlashAttribute("successKey", "movie.create.success");
            return "redirect:/admin/movies/" + created.getMovieId();
        } catch (MovieValidationException ex) {

            bindingResult.reject(ex.getKey());
            model.addAttribute("mode", "create");
            prepareFormDropdowns(model);
            return "admin/movies/movie-form";
        }
    }

    @GetMapping("/{movieId}")
    @PreAuthorize("hasRole('ADMIN') "
            + "or (hasRole('MANAGER') "
            + "and @movieAccessGuard.canRead(authentication, #movieId))")
    public String detail(@PathVariable Integer movieId,
                         Authentication authentication,
                         Model model) {

        MovieDetailView movie = movieService.findById(movieId);

        boolean isAdmin = hasAuthority(authentication, ROLE_ADMIN_AUTHORITY);
        boolean readOnly = !isAdmin;

        model.addAttribute("movie", movie);
        model.addAttribute("readOnly", readOnly);

        return "admin/movies/movie-detail";
    }

    @GetMapping("/{movieId}/edit")
    public String editForm(@PathVariable Integer movieId, Model model) {

        MovieDetailView current = movieService.findById(movieId);

        MovieUpdateRequest request = new MovieUpdateRequest();
        request.setTitle(current.getTitle());
        request.setDuration(current.getDuration());
        request.setDescription(current.getDescription());
        request.setReleaseDate(current.getReleaseDate());
        request.setLanguageId(current.getLanguageId());
        request.setPosterUrl(current.getPosterUrl());
        request.setTrailerUrl(current.getTrailerUrl());
        request.setGenreIds(current.getGenres().stream()
                .map(GenreSummary::getGenreId)
                .collect(Collectors.toSet()));

        model.addAttribute("movie", request);
        model.addAttribute("movieId", movieId);
        model.addAttribute("mode", "edit");
        prepareFormDropdowns(model);

        return "admin/movies/movie-form";
    }

    @PostMapping("/{movieId}")
    public String update(@PathVariable Integer movieId,
                         @Valid @ModelAttribute("movie") MovieUpdateRequest movie,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal CustomUserDetails principal,
                         RedirectAttributes redirectAttributes,
                         Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", "edit");
            model.addAttribute("movieId", movieId);
            prepareFormDropdowns(model);
            return "admin/movies/movie-form";
        }

        uploadPosterIfPresent(movie, bindingResult);
        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", "edit");
            model.addAttribute("movieId", movieId);
            prepareFormDropdowns(model);
            return "admin/movies/movie-form";
        }

        try {
            UpdateResult result = movieService.update(movieId, movie, principal.getUser());

            if (!result.isHasChanges()) {
                model.addAttribute("mode", "edit");
                model.addAttribute("movieId", movieId);
                model.addAttribute("infoKey", "movie.update.no_change");
                prepareFormDropdowns(model);
                return "admin/movies/movie-form";
            }

            redirectAttributes.addFlashAttribute("successKey", "movie.update.success");
            return "redirect:/admin/movies/" + movieId;
        } catch (MovieValidationException ex) {

            bindingResult.reject(ex.getKey());
            model.addAttribute("mode", "edit");
            model.addAttribute("movieId", movieId);
            prepareFormDropdowns(model);
            return "admin/movies/movie-form";
        }
    }

    @PostMapping("/{movieId}/status")
    public String changeStatus(@PathVariable Integer movieId,
                               @Valid @ModelAttribute("statusRequest") MovieStatusChangeRequest request,
                               BindingResult bindingResult,
                               @AuthenticationPrincipal CustomUserDetails principal,
                               RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorKey", "movie.status.invalid");
            return "redirect:/admin/movies/" + movieId;
        }

        try {
            movieService.changeStatus(movieId, request.getStatus(), principal.getUser());

            redirectAttributes.addFlashAttribute("successKey", "movie.status.changed");
        } catch (MovieValidationException ex) {

            redirectAttributes.addFlashAttribute("errorKey", ex.getKey());
        }
        return "redirect:/admin/movies/" + movieId;
    }

    @DeleteMapping("/{movieId}")
    public String deleteHard(@PathVariable Integer movieId,
                             @AuthenticationPrincipal CustomUserDetails principal,
                             RedirectAttributes redirectAttributes) {

        return deleteMovie(movieId, principal, redirectAttributes);
    }

    @PostMapping("/{movieId}/delete")
    public String deleteHardLegacyPost(@PathVariable Integer movieId,
                                       @AuthenticationPrincipal CustomUserDetails principal,
                                       RedirectAttributes redirectAttributes) {

        return deleteMovie(movieId, principal, redirectAttributes);
    }

    private String deleteMovie(Integer movieId,
                               CustomUserDetails principal,
                               RedirectAttributes redirectAttributes) {

        try {
            movieService.deleteHard(movieId, principal.getUser());

            redirectAttributes.addFlashAttribute("successKey", "movie.delete.success");
            return "redirect:/admin/movies";
        } catch (MovieValidationException ex) {

            redirectAttributes.addFlashAttribute("errorKey", ex.getKey());
            return "redirect:/admin/movies/" + movieId;
        }
    }

    @PostMapping("/{movieId}/genres")
    public String addGenre(@PathVariable Integer movieId,
                           @Valid @ModelAttribute("addGenreRequest") AddGenreRequest request,
                           BindingResult bindingResult,
                           @AuthenticationPrincipal CustomUserDetails principal,
                           RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorKey", "movie.genre_not_found");
            return "redirect:/admin/movies/" + movieId;
        }

        try {
            movieService.addGenre(movieId, request.getGenreId(), principal.getUser());

            redirectAttributes.addFlashAttribute("successKey", "movie.genre.added");
        } catch (MovieValidationException ex) {

            redirectAttributes.addFlashAttribute("errorKey", ex.getKey());
        }
        return "redirect:/admin/movies/" + movieId;
    }

    @PostMapping("/{movieId}/genres/{genreId}/remove")
    public String removeGenre(@PathVariable Integer movieId,
                              @PathVariable Integer genreId,
                              @AuthenticationPrincipal CustomUserDetails principal,
                              RedirectAttributes redirectAttributes) {

        try {
            movieService.removeGenre(movieId, genreId, principal.getUser());

            redirectAttributes.addFlashAttribute("successKey", "movie.genre.removed");
        } catch (MovieValidationException ex) {

            redirectAttributes.addFlashAttribute("errorKey", ex.getKey());
        }
        return "redirect:/admin/movies/" + movieId;
    }

    @PostMapping("/{movieId}/branches")
    public String assignBranch(@PathVariable Integer movieId,
                               @Valid @ModelAttribute("assignBranchRequest") AssignBranchRequest request,
                               BindingResult bindingResult,
                               @AuthenticationPrincipal CustomUserDetails principal,
                               RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorKey", "movie.branch_not_found");
            return "redirect:/admin/movies/" + movieId;
        }

        try {
            movieService.assignBranch(movieId, request.getBranchId(), principal.getUser());

            redirectAttributes.addFlashAttribute("successKey", "movie.branch.assigned");
        } catch (MovieValidationException ex) {

            redirectAttributes.addFlashAttribute("errorKey", ex.getKey());
        }
        return "redirect:/admin/movies/" + movieId;
    }

    @GetMapping("/{movieId}/branches")
    public String assignBranchForm(@PathVariable Integer movieId, Model model) {

        MovieDetailView movie = movieService.findById(movieId);

        model.addAttribute("movie", movie);
        model.addAttribute("candidates", branchRepository.findBranchesNotAssignedToMovie(movieId));
        model.addAttribute("assignBranchRequest", new AssignBranchRequest());

        return "admin/movies/assign-branch";
    }

    private void uploadPosterIfPresent(MovieCreateRequest movie, BindingResult bindingResult) {
        if (movie.getPosterFile() == null || movie.getPosterFile().isEmpty()) {
            return;
        }

        try {
            movie.setPosterUrl(cloudinaryImageService.uploadMoviePoster(movie.getPosterFile()));
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("posterFile", ex.getMessage());
        } catch (IllegalStateException ex) {
            bindingResult.rejectValue("posterFile", "movie.poster_upload_failed");
        }
    }

    private void uploadPosterIfPresent(MovieUpdateRequest movie, BindingResult bindingResult) {
        if (movie.getPosterFile() == null || movie.getPosterFile().isEmpty()) {
            return;
        }

        try {
            movie.setPosterUrl(cloudinaryImageService.uploadMoviePoster(movie.getPosterFile()));
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("posterFile", ex.getMessage());
        } catch (IllegalStateException ex) {
            bindingResult.rejectValue("posterFile", "movie.poster_upload_failed");
        }
    }

    @PostMapping("/{movieId}/branches/{branchId}/remove")
    public String unassignBranch(@PathVariable Integer movieId,
                                 @PathVariable Integer branchId,
                                 @AuthenticationPrincipal CustomUserDetails principal,
                                 RedirectAttributes redirectAttributes) {

        try {
            movieService.unassignBranch(movieId, branchId, principal.getUser());

            redirectAttributes.addFlashAttribute("successKey", "movie.branch.unassigned");
        } catch (MovieValidationException ex) {

            redirectAttributes.addFlashAttribute("errorKey", ex.getKey());
        }
        return "redirect:/admin/movies/" + movieId;
    }

    private void prepareFormDropdowns(Model model) {
        model.addAttribute("languages", languageRepository.findAll(Sort.by("name")));
        model.addAttribute("genres", genreRepository.findAllByOrderByNameAsc());
    }

    private String mapSortField(String sort) {
        return switch (sort) {
            case "release_date", "releaseDate" -> "releaseDate";
            case "created_at", "createdAt" -> "createdAt";
            default -> sort;
        };
    }

    private static boolean hasAuthority(Authentication authentication, String authority) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        for (GrantedAuthority granted : authentication.getAuthorities()) {
            if (authority.equals(granted.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
