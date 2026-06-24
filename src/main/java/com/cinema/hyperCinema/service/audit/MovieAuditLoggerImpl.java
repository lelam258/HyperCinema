package com.cinema.hyperCinema.service.audit;

import com.cinema.hyperCinema.dto.admin.movie.response.FieldChange;
import com.cinema.hyperCinema.dto.admin.movie.response.GenreChangeSet;
import com.cinema.hyperCinema.model.AuditLog;
import com.cinema.hyperCinema.model.Movie;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.repository.AuditLogRepository;
import com.cinema.hyperCinema.repository.UserRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Hiện thực {@link MovieAuditLogger}.
 *
 * <p>Mỗi method ghi một bản ghi {@link AuditLog} với {@code entity_type="Movie"},
 * {@code user_id=admin.userId} (REQ 12.5) và chạy trong transaction
 * {@code REQUIRES_NEW} để cô lập lỗi audit (REQ 12.6). Định dạng {@code details}
 * tuân theo bảng B.7.3.</p>
 */
@Service
@RequiredArgsConstructor
public class MovieAuditLoggerImpl implements MovieAuditLogger {

    private static final String ENTITY_TYPE = "Movie";

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logCreate(Movie movie, Set<Integer> genreIds, User admin) {
        save(admin, movie.getMovieId(), "CREATE", toJson(snapshot(movie, genreIds)));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logUpdate(Movie oldMovie, Movie newMovie,
                          List<FieldChange> scalarChanges, GenreChangeSet genreChanges, User admin) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("scalarChanges", scalarChanges);
        if (genreChanges != null && !genreChanges.isEmpty()) {
            Map<String, Object> g = new LinkedHashMap<>();
            g.put("addedGenreIds", genreChanges.getAdded());
            g.put("removedGenreIds", genreChanges.getRemoved());
            body.put("genreChanges", g);
        }
        save(admin, newMovie.getMovieId(), "UPDATE", toJson(body));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logStatusChange(Movie movie, String oldStatus, String newStatus, User admin) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("oldStatus", oldStatus);
        body.put("newStatus", newStatus);
        save(admin, movie.getMovieId(), "STATUS_CHANGE", toJson(body));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAssignBranch(Integer movieId, Integer branchId, User admin) {
        save(admin, movieId, "ASSIGN_BRANCH", toJson(Map.of("branchId", branchId)));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logUnassignBranch(Integer movieId, Integer branchId, User admin) {
        save(admin, movieId, "UNASSIGN_BRANCH", toJson(Map.of("branchId", branchId)));
    }

    private void save(User admin, Integer entityId, String action, String details) {
        AuditLog log = new AuditLog();
        log.setUser(userRepository.getReferenceById(admin.getUserId()));
        log.setEntityType(ENTITY_TYPE);
        log.setEntityId(entityId);
        log.setAction(action);
        log.setDetails(details);
        auditLogRepository.save(log);
    }

    private Map<String, Object> snapshot(Movie m, Set<Integer> genreIds) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", m.getTitle());
        body.put("duration", m.getDuration());
        body.put("description", m.getDescription());
        body.put("releaseDate", m.getReleaseDate());
        body.put("status", m.getStatus());
        body.put("posterUrl", m.getPosterUrl());
        body.put("trailerUrl", m.getTrailerUrl());
        body.put("languageId", m.getLanguageId());
        body.put("genreIds", genreIds == null ? Set.of() : genreIds);
        body.put("createdAt", m.getCreatedAt());
        return body;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException ex) {
            throw new RuntimeException("Failed to serialize audit details", ex);
        }
    }
}
