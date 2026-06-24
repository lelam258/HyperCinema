package com.cinema.hyperCinema.util;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.cinema.hyperCinema.dto.admin.movie.response.GenreChangeSet;

@Component
public class MovieGenreDiffer {

    public GenreChangeSet diff(Set<Integer> currentGenreIds, Set<Integer> requestedGenreIds) {
        Set<Integer> current = currentGenreIds == null ? Set.of() : currentGenreIds;
        Set<Integer> requested = requestedGenreIds == null ? Set.of() : requestedGenreIds;

        Set<Integer> added = new LinkedHashSet<>(requested);
        added.removeAll(current);

        Set<Integer> removed = new LinkedHashSet<>(current);
        removed.removeAll(requested);

        return GenreChangeSet.builder()
                .added(Collections.unmodifiableSet(added))
                .removed(Collections.unmodifiableSet(removed))
                .build();
    }
}
