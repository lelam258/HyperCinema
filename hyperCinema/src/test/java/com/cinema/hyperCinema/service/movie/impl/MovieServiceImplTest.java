package com.cinema.hyperCinema.service.movie.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.cinema.hyperCinema.dto.admin.movie.request.MovieUpdateRequest;
import com.cinema.hyperCinema.dto.admin.movie.response.MovieDetailView;
import com.cinema.hyperCinema.dto.admin.movie.response.UpdateResult;
import com.cinema.hyperCinema.model.Genre;
import com.cinema.hyperCinema.model.Movie;
import com.cinema.hyperCinema.repository.AuditLogRepository;
import com.cinema.hyperCinema.repository.BranchMovieRepository;
import com.cinema.hyperCinema.repository.BranchRepository;
import com.cinema.hyperCinema.repository.FavoriteMovieRepository;
import com.cinema.hyperCinema.repository.GenreRepository;
import com.cinema.hyperCinema.repository.LanguageRepository;
import com.cinema.hyperCinema.repository.MovieGenreRepository;
import com.cinema.hyperCinema.repository.MovieRepository;
import com.cinema.hyperCinema.repository.ShowtimeRepository;
import com.cinema.hyperCinema.service.audit.MovieAuditLogger;
import com.cinema.hyperCinema.util.MovieDiffer;
import com.cinema.hyperCinema.util.MovieGenreDiffer;
import com.cinema.hyperCinema.util.MovieMapper;
import com.cinema.hyperCinema.validation.MovieValidator;

class MovieServiceImplTest {

    @Test
    void updatePersistsGenreOnlyChanges() {
        MovieRepository movieRepository = mock(MovieRepository.class);
        LanguageRepository languageRepository = mock(LanguageRepository.class);
        GenreRepository genreRepository = mock(GenreRepository.class);
        MovieGenreRepository movieGenreRepository = mock(MovieGenreRepository.class);
        BranchRepository branchRepository = mock(BranchRepository.class);
        BranchMovieRepository branchMovieRepository = mock(BranchMovieRepository.class);
        FavoriteMovieRepository favoriteMovieRepository = mock(FavoriteMovieRepository.class);
        ShowtimeRepository showtimeRepository = mock(ShowtimeRepository.class);
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
        MovieAuditLogger auditLogger = mock(MovieAuditLogger.class);
        MovieMapper movieMapper = mock(MovieMapper.class);

        Movie existing = new Movie();
        existing.setMovieId(7);
        existing.setTitle("Dune");
        existing.setDuration(166);
        existing.setDescription("Arrakis");
        existing.setReleaseDate(LocalDate.of(2026, 5, 1));
        existing.setLanguageId(1);

        MovieUpdateRequest request = new MovieUpdateRequest();
        request.setTitle("Dune");
        request.setDuration(166);
        request.setDescription("Arrakis");
        request.setReleaseDate(LocalDate.of(2026, 5, 1));
        request.setLanguageId(1);
        request.setGenreIds(Set.of(1, 2));

        when(movieRepository.findById(7)).thenReturn(Optional.of(existing));
        when(languageRepository.existsById(1)).thenReturn(true);
        when(genreRepository.countByGenreIdIn(Set.of(1, 2))).thenReturn(2L);
        when(movieGenreRepository.findGenreIdsByMovieId(7)).thenReturn(Set.of(1));
        when(genreRepository.getReferenceById(2)).thenReturn(new Genre());
        when(movieMapper.toDetailView(existing)).thenReturn(MovieDetailView.builder().movieId(7).build());

        MovieValidator validator = new MovieValidator(
                movieRepository, languageRepository, genreRepository, movieGenreRepository, showtimeRepository);
        MovieServiceImpl service = new MovieServiceImpl(
                movieRepository,
                languageRepository,
                genreRepository,
                movieGenreRepository,
                branchRepository,
                branchMovieRepository,
                favoriteMovieRepository,
                showtimeRepository,
                auditLogRepository,
                validator,
                auditLogger,
                new MovieDiffer(),
                new MovieGenreDiffer(),
                movieMapper);

        UpdateResult result = service.update(7, request, null);

        assertThat(result.isHasChanges()).isTrue();
        assertThat(result.getGenreChanges().getAdded()).containsExactly(2);
        verify(movieGenreRepository).saveAll(any());
        verify(movieRepository, never()).save(any(Movie.class));
    }
}
