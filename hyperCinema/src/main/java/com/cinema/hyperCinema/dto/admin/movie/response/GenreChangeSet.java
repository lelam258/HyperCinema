package com.cinema.hyperCinema.dto.admin.movie.response;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenreChangeSet {

    private Set<Integer> added;

    private Set<Integer> removed;

    public boolean isEmpty() {
        return added.isEmpty() && removed.isEmpty();
    }
}
