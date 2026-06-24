package com.cinema.hyperCinema.repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.cinema.hyperCinema.model.Movie;
import com.cinema.hyperCinema.model.MovieGenre;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

public final class MovieSpecifications {

    private static final Set<String> ALLOWED_STATUSES =
            Set.of("ComingSoon", "NowShowing", "Ended");

    private MovieSpecifications() {
    }

    public static Specification<Movie> matches(String keyword,
                                               String status,
                                               Integer languageId,
                                               Integer genreId,
                                               LocalDate releaseDateFrom,
                                               LocalDate releaseDateTo) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            String normalizedKeyword = trimToNull(keyword);
            if (normalizedKeyword != null) {
                predicates.add(cb.like(cb.lower(root.get("title")),
                        "%" + normalizedKeyword.toLowerCase() + "%"));
            }

            String normalizedStatus = trimToNull(status);
            if (normalizedStatus != null && ALLOWED_STATUSES.contains(normalizedStatus)) {
                predicates.add(cb.equal(root.get("status"), normalizedStatus));
            }

            if (languageId != null && languageId > 0) {
                predicates.add(cb.equal(root.get("languageId"), languageId));
            }

            if (genreId != null && genreId > 0) {
                Subquery<Integer> subquery = query.subquery(Integer.class);
                Root<MovieGenre> movieGenre = subquery.from(MovieGenre.class);
                subquery.select(movieGenre.get("id").get("movieId"))
                        .where(cb.equal(movieGenre.get("id").get("genreId"), genreId));
                predicates.add(root.get("movieId").in(subquery));
            }

            if (releaseDateFrom != null && releaseDateTo != null) {
                predicates.add(cb.between(root.get("releaseDate"), releaseDateFrom, releaseDateTo));
            }

            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
