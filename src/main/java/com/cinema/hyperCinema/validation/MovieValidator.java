package com.cinema.hyperCinema.validation;

import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.cinema.hyperCinema.dto.admin.movie.request.MovieCreateRequest;
import com.cinema.hyperCinema.dto.admin.movie.request.MovieUpdateRequest;
import com.cinema.hyperCinema.exception.movie.MovieValidationException;
import com.cinema.hyperCinema.repository.GenreRepository;
import com.cinema.hyperCinema.repository.LanguageRepository;
import com.cinema.hyperCinema.repository.MovieGenreRepository;
import com.cinema.hyperCinema.repository.MovieRepository;
import com.cinema.hyperCinema.repository.ShowtimeRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MovieValidator {

    private static final Set<String> ALLOWED_STATUSES =
            Set.of("ComingSoon", "NowShowing", "Ended");

    private static final long MAX_GENRES_PER_MOVIE = 10L;

    private final MovieRepository movieRepo;
    private final LanguageRepository languageRepo;
    private final GenreRepository genreRepo;
    private final MovieGenreRepository movieGenreRepo;
    private final ShowtimeRepository showtimeRepo;

    public void validateCreate(MovieCreateRequest req) {
        validateLanguageExists(req.getLanguageId());
        validateAllGenresExist(req.getGenreIds());
        validateGenreCountUpperBound(0L, sizeOf(req.getGenreIds()));
        if (movieRepo.existsByTitleIgnoreCaseAndReleaseDate(
                req.getTitle().trim(), req.getReleaseDate())) {
            throw new MovieValidationException("movie.duplicate_title_on_release_date");
        }
    }

    public void validateUpdate(Integer id, MovieUpdateRequest req) {
        validateLanguageExists(req.getLanguageId());
        validateAllGenresExist(req.getGenreIds());
        validateGenreCountUpperBound(0L, sizeOf(req.getGenreIds()));
        if (movieRepo.existsByTitleIgnoreCaseAndReleaseDateAndMovieIdNot(
                req.getTitle().trim(), req.getReleaseDate(), id)) {
            throw new MovieValidationException("movie.duplicate_title_on_release_date");
        }
    }

    public void validateNoDuplicateTitleOnReleaseDate(
            String title, java.time.LocalDate releaseDate, Integer excludeId) {
        String normalized = title == null ? null : title.trim();
        boolean duplicate = excludeId == null
                ? movieRepo.existsByTitleIgnoreCaseAndReleaseDate(normalized, releaseDate)
                : movieRepo.existsByTitleIgnoreCaseAndReleaseDateAndMovieIdNot(
                        normalized, releaseDate, excludeId);
        if (duplicate) {
            throw new MovieValidationException("movie.duplicate_title_on_release_date");
        }
    }

    public void validateLanguageExists(Integer languageId) {
        if (languageId == null || !languageRepo.existsById(languageId)) {
            throw new MovieValidationException("movie.language_not_found");
        }
    }

    public void validateAllGenresExist(Set<Integer> genreIds) {
        if (genreIds == null || genreIds.isEmpty()) {
            return;
        }
        long existCount = genreRepo.countByGenreIdIn(genreIds);
        if (existCount != genreIds.size()) {
            throw new MovieValidationException("movie.genre_not_found");
        }
    }

    public void validateGenreCountUpperBound(long currentCount, long addCount) {
        if (currentCount + addCount > MAX_GENRES_PER_MOVIE) {
            throw new MovieValidationException("movie.genre_limit_exceeded");
        }
    }

    public void validateStatusValue(String status) {
        if (status == null || !ALLOWED_STATUSES.contains(status)) {
            throw new MovieValidationException("movie.status.invalid", HttpStatus.BAD_REQUEST);
        }
    }

    private static long sizeOf(Set<Integer> s) {
        return s == null ? 0L : s.size();
    }
}
