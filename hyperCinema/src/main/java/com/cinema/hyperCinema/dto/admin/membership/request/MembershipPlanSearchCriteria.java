package com.cinema.hyperCinema.dto.admin.membership.request;

import java.util.Set;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MembershipPlanSearchCriteria {

    private static final Set<String> ALLOWED_SORTS = Set.of("level", "name", "discountPercent", "price", "status", "planId");
    private static final Set<String> ALLOWED_DIRECTIONS = Set.of("ASC", "DESC");
    private static final Set<String> ALLOWED_STATUSES = Set.of("ACTIVE", "INACTIVE");

    private String keyword;
    private String status;

    @PositiveOrZero
    private int page = 0;

    @Min(1)
    private int size = 20;

    private String sort = "level";
    private String direction = "ASC";

    public MembershipPlanSearchCriteria normalize() {
        keyword = normalizeBlank(keyword);
        if (status != null) {
            String normalized = status.trim().toUpperCase();
            status = ALLOWED_STATUSES.contains(normalized) ? normalized : null;
        }
        if (page < 0) {
            page = 0;
        }
        if (size < 1) {
            size = 20;
        }
        if (sort == null || !ALLOWED_SORTS.contains(sort)) {
            sort = "level";
        }
        if (direction == null || !ALLOWED_DIRECTIONS.contains(direction.trim().toUpperCase())) {
            direction = "ASC";
        } else {
            direction = direction.trim().toUpperCase();
        }
        return this;
    }

    private static String normalizeBlank(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
