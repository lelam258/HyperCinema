package com.cinema.hyperCinema.service.movie.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.cinema.hyperCinema.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.hyperCinema.dto.admin.movie.request.MovieCreateRequest;
import com.cinema.hyperCinema.dto.admin.movie.request.MovieSearchCriteria;
import com.cinema.hyperCinema.dto.admin.movie.request.MovieUpdateRequest;
import com.cinema.hyperCinema.dto.admin.movie.response.FieldChange;
import com.cinema.hyperCinema.dto.admin.movie.response.GenreChangeSet;
import com.cinema.hyperCinema.dto.admin.movie.response.MovieDetailView;
import com.cinema.hyperCinema.dto.admin.movie.response.MovieListItem;
import com.cinema.hyperCinema.dto.admin.movie.response.UpdateResult;
import com.cinema.hyperCinema.exception.movie.MovieNotFoundException;
import com.cinema.hyperCinema.exception.movie.MovieValidationException;
import com.cinema.hyperCinema.repository.AuditLogRepository;
import com.cinema.hyperCinema.repository.BranchMovieRepository;
import com.cinema.hyperCinema.repository.BranchRepository;
import com.cinema.hyperCinema.repository.FavoriteMovieRepository;
import com.cinema.hyperCinema.repository.GenreRepository;
import com.cinema.hyperCinema.repository.LanguageRepository;
import com.cinema.hyperCinema.repository.MovieGenreRepository;
import com.cinema.hyperCinema.repository.MovieRepository;
import com.cinema.hyperCinema.repository.MovieSpecifications;
import com.cinema.hyperCinema.repository.ShowtimeRepository;
import com.cinema.hyperCinema.service.audit.MovieAuditLogger;
import com.cinema.hyperCinema.service.movie.MovieService;
import com.cinema.hyperCinema.util.MovieDiffer;
import com.cinema.hyperCinema.util.MovieGenreDiffer;
import com.cinema.hyperCinema.util.MovieMapper;
import com.cinema.hyperCinema.validation.MovieValidator;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class MovieServiceImpl implements MovieService {

    private static final Logger log = LoggerFactory.getLogger(MovieServiceImpl.class);

    private final MovieRepository movieRepository;
    private final LanguageRepository languageRepository;
    private final GenreRepository genreRepository;
    private final MovieGenreRepository movieGenreRepository;
    private final BranchRepository branchRepository;
    private final BranchMovieRepository branchMovieRepository;
    private final FavoriteMovieRepository favoriteMovieRepository;
    private final ShowtimeRepository showtimeRepository;
    private final AuditLogRepository auditLogRepository;
    private final MovieValidator validator;
    private final MovieAuditLogger auditLogger;
    private final MovieDiffer movieDiffer;
    private final MovieGenreDiffer movieGenreDiffer;
    private final MovieMapper movieMapper;

    @Override
    @Transactional(readOnly = true)
    public MovieDetailView findById(Integer movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new MovieNotFoundException(movieId));
        return movieMapper.toDetailView(movie);
    }

    @Override
    public MovieDetailView create(MovieCreateRequest req, User admin) {

        validator.validateCreate(req);

        Movie movie = new Movie();
        movie.setTitle(req.getTitle() == null ? null : req.getTitle().trim());
        movie.setDuration(req.getDuration());
        movie.setDescription(req.getDescription());
        movie.setReleaseDate(req.getReleaseDate());
        movie.setLanguageId(req.getLanguageId());
        movie.setPosterUrl(req.getPosterUrl());
        movie.setTrailerUrl(req.getTrailerUrl());
        movie.setStatus(req.getStatus() != null ? req.getStatus() : "ComingSoon");
        movie.setCreatedAt(LocalDateTime.now());

        Movie saved = movieRepository.save(movie);

        Set<Integer> genreIds = req.getGenreIds();
        if (genreIds != null && !genreIds.isEmpty()) {
            movieGenreRepository.saveAll(toMovieGenres(saved, genreIds));
        }

        auditSafe(() -> auditLogger.logCreate(saved, genreIds, admin));

        return movieMapper.toDetailView(saved);
    }

    @Override
    public UpdateResult update(Integer movieId, MovieUpdateRequest req, User admin) {

        Movie existing = movieRepository.findById(movieId)
                .orElseThrow(() -> new MovieNotFoundException(movieId));

        validator.validateUpdate(movieId, req);

        List<FieldChange> scalarChanges = movieDiffer.diff(existing, req);
        Set<Integer> currentGenreIds = movieGenreRepository.findGenreIdsByMovieId(movieId);
        GenreChangeSet genreChanges = movieGenreDiffer.diff(currentGenreIds, req.getGenreIds());

        if (scalarChanges.isEmpty() && genreChanges.isEmpty()) {
            return UpdateResult.builder()
                    .movie(movieMapper.toDetailView(existing))
                    .hasChanges(false)
                    .scalarChanges(List.of())
                    .genreChanges(GenreChangeSet.builder()
                            .added(Set.of())
                            .removed(Set.of())
                            .build())
                    .build();
        }

        if (!scalarChanges.isEmpty()) {
            existing.setTitle(req.getTitle() == null ? null : req.getTitle().trim());
            existing.setDuration(req.getDuration());
            existing.setDescription(req.getDescription());
            existing.setReleaseDate(req.getReleaseDate());
            existing.setLanguageId(req.getLanguageId());
            existing.setPosterUrl(req.getPosterUrl());
            existing.setTrailerUrl(req.getTrailerUrl());
        }
        existing.setUpdatedAt(LocalDateTime.now());

        Movie saved = scalarChanges.isEmpty() ? existing : movieRepository.save(existing);

        if (!genreChanges.isEmpty()) {
            Set<Integer> removed = genreChanges.getRemoved();
            if (removed != null && !removed.isEmpty()) {
                movieGenreRepository.deleteByMovieIdAndGenreIdIn(movieId, removed);
            }
            Set<Integer> added = genreChanges.getAdded();
            if (added != null && !added.isEmpty()) {
                movieGenreRepository.saveAll(toMovieGenres(saved, added));
            }
        }

        auditSafe(() -> auditLogger.logUpdate(existing, saved, scalarChanges, genreChanges, admin));

        return UpdateResult.builder()
                .movie(movieMapper.toDetailView(saved))
                .hasChanges(true)
                .scalarChanges(scalarChanges)
                .genreChanges(genreChanges)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MovieListItem> search(MovieSearchCriteria criteria, Pageable pageable) {
        Specification<Movie> spec = MovieSpecifications.matches(
                criteria.getKeyword(),
                criteria.getStatus(),
                criteria.getLanguageId(),
                criteria.getGenreId(),
                criteria.getReleaseDateFrom(),
                criteria.getReleaseDateTo());

        Page<Movie> page = movieRepository.findAll(spec, pageable);
        return page.map(movieMapper::toListItem);
    }

    @Override
    public void changeStatus(Integer movieId, String newStatus, User admin) {

        validator.validateStatusValue(newStatus);

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new MovieNotFoundException(movieId));

        String oldStatus = movie.getStatus();
        if (oldStatus.equals(newStatus)) {
            return;
        }

        if ("ComingSoon".equals(oldStatus) && "Ended".equals(newStatus)
                && !hasEverBeenNowShowing(movieId)) {
            throw new MovieValidationException("movie.invalid_status_transition");
        }

        if ("Ended".equals(newStatus)
                && showtimeRepository.existsByMovie_MovieIdAndStartTimeAfter(movieId, LocalDateTime.now())) {
            throw new MovieValidationException("movie.cannot_end_with_future_showtimes");
        }

        movie.setStatus(newStatus);
        movie.setUpdatedAt(LocalDateTime.now());
        movieRepository.save(movie);

        auditSafe(() -> auditLogger.logStatusChange(movie, oldStatus, newStatus, admin));
    }

    private boolean hasEverBeenNowShowing(Integer movieId) {
        return auditLogRepository.existsByEntityTypeAndEntityIdAndActionAndDetailsLike(
                "Movie", movieId, "STATUS_CHANGE", "%\"newStatus\":\"NowShowing\"%");
    }

    @Override
    public void deleteHard(Integer movieId, User admin) {

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new MovieNotFoundException(movieId));

        if (showtimeRepository.existsByMovie_MovieIdAndStartTimeAfter(movieId, LocalDateTime.now())) {
            throw new MovieValidationException("movie.cannot_end_with_future_showtimes");
        }

        String oldStatus = movie.getStatus();
        if ("Ended".equals(oldStatus)) {
            return;
        }
        movie.setStatus("Ended");
        movie.setUpdatedAt(LocalDateTime.now());
        movieRepository.save(movie);
        auditSafe(() -> auditLogger.logStatusChange(movie, oldStatus, "Ended", admin));
    }

    @Override
    public void addGenre(Integer movieId, Integer genreId, User admin) {

        if (!movieRepository.existsById(movieId)) {
            throw new MovieNotFoundException(movieId);
        }
        if (!genreRepository.existsById(genreId)) {
            throw new MovieValidationException("movie.genre_not_found");
        }
        if (movieGenreRepository.existsByIdMovieIdAndIdGenreId(movieId, genreId)) {
            return;
        }

        long currentCount = movieGenreRepository.countByIdMovieId(movieId);
        validator.validateGenreCountUpperBound(currentCount, 1L);

        movieGenreRepository.save(buildMovieGenre(movieId, genreId));
    }

    @Override
    public void removeGenre(Integer movieId, Integer genreId, User admin) {

        if (!movieRepository.existsById(movieId)) {
            throw new MovieNotFoundException(movieId);
        }
        if (!genreRepository.existsById(genreId)) {
            throw new MovieValidationException("movie.genre_not_found");
        }
        if (!movieGenreRepository.existsByIdMovieIdAndIdGenreId(movieId, genreId)) {
            return;
        }

        movieGenreRepository.deleteByIdMovieIdAndIdGenreId(movieId, genreId);
    }

    @Override
    public void assignBranch(Integer movieId, Integer branchId, User admin) {

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new MovieNotFoundException(movieId));

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new MovieValidationException("movie.branch_not_found"));
        if ("Ended".equals(movie.getStatus())) {
            throw new MovieValidationException("movie.cannot_assign_ended_movie");
        }
        if (!"Active".equalsIgnoreCase(branch.getStatus())) {
            throw new MovieValidationException("movie.branch_inactive");
        }

        BranchMovieId pkId = new BranchMovieId(branchId, movieId);
        if (branchMovieRepository.existsById(pkId)) {
            return;
        }

        BranchMovie bm = new BranchMovie();
        bm.setId(pkId);
        bm.setBranch(branch);
        bm.setMovie(movie);
        bm.setAssignedAt(LocalDateTime.now());
        branchMovieRepository.save(bm);

        auditSafe(() -> auditLogger.logAssignBranch(movieId, branchId, admin));
    }

    @Override
    public void unassignBranch(Integer movieId, Integer branchId, User admin) {

        BranchMovieId pkId = new BranchMovieId(branchId, movieId);
        if (!branchMovieRepository.existsById(pkId)) {
            return;
        }

        if (showtimeRepository.existsByMovie_MovieIdAndHall_Branch_BranchIdAndStartTimeAfter(
                movieId, branchId, LocalDateTime.now())) {
            throw new MovieValidationException("movie.cannot_unassign_with_future_showtimes");
        }

        branchMovieRepository.deleteById(pkId);
        auditSafe(() -> auditLogger.logUnassignBranch(movieId, branchId, admin));
    }

    private List<MovieGenre> toMovieGenres(Movie movie, Set<Integer> genreIds) {
        List<MovieGenre> links = new ArrayList<>(genreIds.size());
        for (Integer genreId : genreIds) {
            MovieGenre link = new MovieGenre();
            link.setId(new MovieGenreId(movie.getMovieId(), genreId));
            link.setMovie(movie);
            link.setGenre(genreRepository.getReferenceById(genreId));
            links.add(link);
        }
        return links;
    }

    private MovieGenre buildMovieGenre(Integer movieId, Integer genreId) {
        MovieGenre link = new MovieGenre();
        link.setId(new MovieGenreId(movieId, genreId));
        link.setMovie(movieRepository.getReferenceById(movieId));
        link.setGenre(genreRepository.getReferenceById(genreId));
        return link;
    }

    private void auditSafe(Runnable auditAction) {
        try {
            auditAction.run();
        } catch (Throwable auditEx) {
            try {
                log.warn("Movie audit log failed: {}", auditEx.getMessage(), auditEx);
            } catch (Throwable ignored) {
            }
        }
    }
}
