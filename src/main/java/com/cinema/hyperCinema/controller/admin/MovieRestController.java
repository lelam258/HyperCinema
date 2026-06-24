package com.cinema.hyperCinema.controller.admin;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cinema.hyperCinema.dto.admin.movie.request.MovieCreateRequest;
import com.cinema.hyperCinema.dto.admin.movie.request.MovieUpdateRequest;
import com.cinema.hyperCinema.dto.admin.movie.response.MovieDetailView;
import com.cinema.hyperCinema.dto.admin.movie.response.UpdateResult;
import com.cinema.hyperCinema.exception.movie.MovieNotFoundException;
import com.cinema.hyperCinema.exception.movie.MovieValidationException;
import com.cinema.hyperCinema.security.CustomUserDetails;
import com.cinema.hyperCinema.service.movie.MovieService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/movies")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class MovieRestController {

    private final MovieService movieService;
    private final MessageSource messageSource;

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody MovieCreateRequest request,
                                    BindingResult bindingResult,
                                    @AuthenticationPrincipal CustomUserDetails principal,
                                    Locale locale) {
        if (bindingResult.hasErrors()) {
            return validationError(bindingResult);
        }

        try {
            MovieDetailView created = movieService.create(request, principal.getUser());
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", resolveMessage("movie.create.success", locale),
                    "movieId", created.getMovieId(),
                    "redirectUrl", "/admin/movies/" + created.getMovieId()));
        } catch (MovieValidationException ex) {
            return businessError(ex, locale);
        }
    }

    @PutMapping("/{movieId}")
    public ResponseEntity<?> update(@PathVariable Integer movieId,
                                    @Valid @RequestBody MovieUpdateRequest request,
                                    BindingResult bindingResult,
                                    @AuthenticationPrincipal CustomUserDetails principal,
                                    Locale locale) {
        if (bindingResult.hasErrors()) {
            return validationError(bindingResult);
        }

        try {
            UpdateResult result = movieService.update(movieId, request, principal.getUser());
            String messageKey = result.isHasChanges()
                    ? "movie.update.success"
                    : "movie.update.no_change";

            return ResponseEntity.ok(Map.of(
                    "message", resolveMessage(messageKey, locale),
                    "movieId", movieId,
                    "redirectUrl", "/admin/movies/" + movieId,
                    "hasChanges", result.isHasChanges()));
        } catch (MovieNotFoundException ex) {
            return notFoundError(ex, locale);
        } catch (MovieValidationException ex) {
            return businessError(ex, locale);
        }
    }

    private ResponseEntity<?> validationError(BindingResult bindingResult) {
        List<Map<String, String>> fieldErrors = bindingResult.getFieldErrors().stream()
                .map(this::fieldErrorBody)
                .toList();

        return ResponseEntity.badRequest().body(Map.of(
                "message", "Dữ liệu không hợp lệ.",
                "errors", fieldErrors));
    }

    private Map<String, String> fieldErrorBody(FieldError error) {
        return Map.of(
                "field", error.getField(),
                "message", error.getDefaultMessage() == null
                        ? "Giá trị không hợp lệ."
                        : error.getDefaultMessage());
    }

    private ResponseEntity<?> businessError(MovieValidationException ex, Locale locale) {
        return ResponseEntity.badRequest().body(Map.of(
                "message", resolveMessage(ex.getKey(), locale),
                "code", ex.getKey()));
    }

    private ResponseEntity<?> notFoundError(MovieNotFoundException ex, Locale locale) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "message", resolveMessage(ex.getKey(), locale),
                "code", ex.getKey()));
    }

    private String resolveMessage(String key, Locale locale) {
        return messageSource.getMessage(key, null, key, locale);
    }
}
