package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.Payment;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public final class PaymentSpecifications {

    private PaymentSpecifications() {}

    public static Specification<Payment> filter(
            String status,
            String method,
            LocalDate startDate,
            LocalDate endDate,
            Integer branchId,
            Integer userId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null && !status.trim().isEmpty() && !"all".equalsIgnoreCase(status)) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (method != null && !method.trim().isEmpty() && !"all".equalsIgnoreCase(method)) {
                predicates.add(cb.equal(root.get("method"), method));
            }

            if (startDate != null) {
                LocalDateTime startDateTime = startDate.atStartOfDay();
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDateTime));
            }

            if (endDate != null) {
                LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDateTime));
            }

            if (branchId != null) {
                predicates.add(cb.equal(
                    root.get("booking").get("showtime").get("hall").get("branch").get("branchId"),
                    branchId
                ));
            }

            if (userId != null) {
                predicates.add(cb.equal(
                    root.get("booking").get("user").get("userId"),
                    userId
                ));
            }

            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
