package com.cinema.hyperCinema.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.cinema.hyperCinema.model.Branch;
import org.springframework.data.jpa.domain.Specification;

import com.cinema.hyperCinema.dto.admin.branch.request.BranchSearchCriteria;

import jakarta.persistence.criteria.Predicate;

public final class BranchSpecifications {

    private static final Set<String> ALLOWED_STATUSES =
            Set.of("Active", "Inactive", "Maintenance");

    private BranchSpecifications() {

    }

    public static Specification<Branch> matches(BranchSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria != null) {
                String keyword = trimToNull(criteria.getKeyword());
                if (keyword != null) {
                    String pattern = "%" + keyword.toLowerCase() + "%";
                    Predicate nameMatch = cb.like(cb.lower(root.get("name")), pattern);
                    Predicate addressMatch = cb.like(cb.lower(root.get("address")), pattern);
                    predicates.add(cb.or(nameMatch, addressMatch));
                }

                String city = trimToNull(criteria.getCity());
                if (city != null) {
                    predicates.add(cb.equal(cb.lower(root.get("city")), city.toLowerCase()));
                }

                String status = trimToNull(criteria.getStatus());
                if (status != null && ALLOWED_STATUSES.contains(status)) {
                    predicates.add(cb.equal(root.get("status"), status));
                }

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
