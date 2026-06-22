package com.cinema.hyperCinema.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.cinema.hyperCinema.model.Movie;
import org.springframework.stereotype.Component;

import com.cinema.hyperCinema.dto.admin.movie.request.MovieUpdateRequest;
import com.cinema.hyperCinema.dto.admin.movie.response.FieldChange;

@Component
public class MovieDiffer {

    public List<FieldChange> diff(Movie existing, MovieUpdateRequest req) {
        Objects.requireNonNull(existing, "existing movie must not be null");
        Objects.requireNonNull(req, "update request must not be null");

        List<FieldChange> changes = new ArrayList<>();
        compare("title", existing.getTitle(), normalizeTitle(req.getTitle()), changes);
        compare("duration", existing.getDuration(), req.getDuration(), changes);
        compare("description", existing.getDescription(), req.getDescription(), changes);
        compare("releaseDate", existing.getReleaseDate(), req.getReleaseDate(), changes);
        compare("languageId", existing.getLanguageId(), req.getLanguageId(), changes);
        compare("posterUrl", existing.getPosterUrl(), req.getPosterUrl(), changes);
        compare("trailerUrl", existing.getTrailerUrl(), req.getTrailerUrl(), changes);
        return Collections.unmodifiableList(changes);
    }

    private static String normalizeTitle(String title) {
        return title == null ? null : title.trim();
    }

    private static void compare(String field, Object oldValue, Object newValue,
                                List<FieldChange> changes) {
        if (!Objects.equals(oldValue, newValue)) {
            changes.add(new FieldChange(field, oldValue, newValue));
        }
    }
}
