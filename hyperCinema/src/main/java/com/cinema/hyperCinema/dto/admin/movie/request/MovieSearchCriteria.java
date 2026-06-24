package com.cinema.hyperCinema.dto.admin.movie.request;

import java.time.LocalDate;
import java.util.Set;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MovieSearchCriteria {

    public static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("title", "releaseDate", "status", "createdAt");

    public static final Set<String> ALLOWED_DIRECTIONS = Set.of("ASC", "DESC");

    public static final String DEFAULT_SORT = "createdAt";

    public static final String DEFAULT_DIRECTION = "DESC";

    public static final int DEFAULT_PAGE = 0;

    public static final int DEFAULT_SIZE = 10;

    private String keyword;

    private String status;

    private Integer languageId;

    private Integer genreId;

    private LocalDate releaseDateFrom;

    private LocalDate releaseDateTo;

    @PositiveOrZero
    private Integer page = DEFAULT_PAGE;

    @Min(1)
    private Integer size = DEFAULT_SIZE;

    private String sort = DEFAULT_SORT;

    private String direction = DEFAULT_DIRECTION;

    public MovieSearchCriteria normalize() {
        if (page == null || page < 0) {
            page = DEFAULT_PAGE;
        }
        if (size == null || size < 1) {
            size = DEFAULT_SIZE;
        }
        if (sort == null || !ALLOWED_SORT_FIELDS.contains(sort)) {
            sort = DEFAULT_SORT;
        }
        if (direction == null) {
            direction = DEFAULT_DIRECTION;
        } else {
            String norm = direction.trim().toUpperCase();
            direction = ALLOWED_DIRECTIONS.contains(norm) ? norm : DEFAULT_DIRECTION;
        }
        return this;
    }
}
