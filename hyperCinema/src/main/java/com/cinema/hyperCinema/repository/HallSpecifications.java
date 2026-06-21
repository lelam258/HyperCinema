package com.cinema.hyperCinema.repository;

import java.util.ArrayList;
import java.util.List;

import com.cinema.hyperCinema.model.Branch;
import com.cinema.hyperCinema.model.Hall;
import org.springframework.data.jpa.domain.Specification;

import com.cinema.hyperCinema.dto.admin.hall.request.HallSearchCriteria;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;

public final class HallSpecifications {

    private HallSpecifications() {

    }

    public static Specification<Hall> matches(HallSearchCriteria criteria, Integer forcedBranchId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<Hall, Branch> branch = root.join("branch");

            if (forcedBranchId != null) {
                predicates.add(cb.equal(branch.get("branchId"), forcedBranchId));
            } else if (criteria != null && criteria.getBranchId() != null) {
                predicates.add(cb.equal(branch.get("branchId"), criteria.getBranchId()));
            }

            if (criteria != null) {
                String keyword = trimToNull(criteria.getKeyword());
                if (keyword != null) {
                    String pattern = "%" + keyword.toLowerCase() + "%";
                    Predicate hallName = cb.like(cb.lower(root.get("name")), pattern);
                    Predicate branchName = cb.like(cb.lower(branch.get("name")), pattern);
                    predicates.add(cb.or(hallName, branchName));
                }

                String city = trimToNull(criteria.getCity());
                if (city != null) {
                    predicates.add(cb.equal(cb.lower(branch.get("city")), city.toLowerCase()));
                }

                String hallType = trimToNull(criteria.getHallType());
                if (hallType != null) {
                    predicates.add(cb.equal(cb.lower(root.get("hallType")), hallType.toLowerCase()));
                }

                String status = trimToNull(criteria.getStatus());
                if (status != null) {
                    predicates.add(cb.equal(cb.lower(root.get("status")), status.toLowerCase()));
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
