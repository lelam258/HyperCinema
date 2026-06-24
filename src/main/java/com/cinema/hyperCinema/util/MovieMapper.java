package com.cinema.hyperCinema.util;

import java.time.LocalDateTime;
import java.util.List;

import com.cinema.hyperCinema.model.Branch;
import com.cinema.hyperCinema.model.BranchMovie;
import com.cinema.hyperCinema.model.Genre;
import com.cinema.hyperCinema.model.Movie;
import org.springframework.stereotype.Component;

import com.cinema.hyperCinema.dto.admin.movie.response.BranchAssignmentSummary;
import com.cinema.hyperCinema.dto.admin.movie.response.GenreSummary;
import com.cinema.hyperCinema.dto.admin.movie.response.MovieDetailView;
import com.cinema.hyperCinema.dto.admin.movie.response.MovieListItem;
import com.cinema.hyperCinema.repository.BranchMovieRepository;
import com.cinema.hyperCinema.repository.LanguageRepository;
import com.cinema.hyperCinema.repository.MovieGenreRepository;
import com.cinema.hyperCinema.repository.ShowtimeRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MovieMapper {

    private final MovieGenreRepository movieGenreRepository;
    private final BranchMovieRepository branchMovieRepository;
    private final ShowtimeRepository showtimeRepository;
    private final LanguageRepository languageRepository;

    public MovieDetailView toDetailView(Movie movie) {
        Integer movieId = movie.getMovieId();
        LocalDateTime now = LocalDateTime.now();

        List<GenreSummary> genres = movieGenreRepository.findGenresByMovieId(movieId)
                .stream()
                .map(MovieMapper::toGenreSummary)
                .toList();

        List<BranchAssignmentSummary> branches = branchMovieRepository
                .findByMovieIdWithBranch(movieId)
                .stream()
                .map(MovieMapper::toBranchAssignmentSummary)
                .toList();

        long futureShowtimeCount =
                showtimeRepository.countByMovie_MovieIdAndStartTimeAfter(movieId, now);
        long pastShowtimeCount =
                showtimeRepository.countByMovie_MovieIdAndStartTimeLessThanEqual(movieId, now);

        return MovieDetailView.builder()
                .movieId(movie.getMovieId())
                .title(movie.getTitle())
                .duration(movie.getDuration())
                .description(movie.getDescription())
                .releaseDate(movie.getReleaseDate())
                .status(movie.getStatus())
                .posterUrl(movie.getPosterUrl())
                .trailerUrl(movie.getTrailerUrl())
                .languageId(movie.getLanguageId())
                .languageName(resolveLanguageName(movie))
                .genres(genres)
                .branches(branches)
                .futureShowtimeCount(futureShowtimeCount)
                .pastShowtimeCount(pastShowtimeCount)
                .createdAt(movie.getCreatedAt())
                .updatedAt(movie.getUpdatedAt())
                .build();
    }

    public MovieListItem toListItem(Movie movie) {
        List<String> genreNames = movieGenreRepository.findGenresByMovieId(movie.getMovieId())
                .stream()
                .map(Genre::getName)
                .toList();

        return MovieListItem.builder()
                .movieId(movie.getMovieId())
                .title(movie.getTitle())
                .duration(movie.getDuration())
                .languageName(resolveLanguageName(movie))
                .genreNames(genreNames)
                .releaseDate(movie.getReleaseDate())
                .status(movie.getStatus())
                .posterUrl(movie.getPosterUrl())
                .createdAt(movie.getCreatedAt())
                .build();
    }

    private String resolveLanguageName(Movie movie) {
        if (movie.getLanguage() != null && movie.getLanguage().getName() != null) {
            return movie.getLanguage().getName();
        }
        Integer languageId = movie.getLanguageId();
        if (languageId == null) {
            return null;
        }
        return languageRepository.findById(languageId)
                .map(language -> language.getName())
                .orElse(null);
    }

    private static GenreSummary toGenreSummary(Genre genre) {
        return GenreSummary.builder()
                .genreId(genre.getGenreId())
                .name(genre.getName())
                .build();
    }

    private static BranchAssignmentSummary toBranchAssignmentSummary(BranchMovie branchMovie) {
        Branch branch = branchMovie.getBranch();
        return BranchAssignmentSummary.builder()
                .branchId(branch == null ? null : branch.getBranchId())
                .branchName(branch == null ? null : branch.getName())
                .city(branch == null ? null : branch.getCity())
                .assignedAt(branchMovie.getAssignedAt())
                .build();
    }
}
