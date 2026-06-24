package com.cinema.hyperCinema.dto.admin.voucher.request;

import java.util.Set;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VoucherSearchCriteria {

    public static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("createdAt", "code", "title", "startDate", "endDate", "status");

    public static final Set<String> ALLOWED_DIRECTIONS = Set.of("ASC", "DESC");

    public static final Set<String> ALLOWED_STATUSES = Set.of("ACTIVE", "INACTIVE", "EXPIRED");

    public static final String DEFAULT_SORT = "createdAt";

    public static final String DEFAULT_DIRECTION = "DESC";

    public static final int DEFAULT_PAGE = 0;

    public static final int DEFAULT_SIZE = 20;

    private String keyword;     // khớp code hoặc title

    private String status;      // ACTIVE | INACTIVE | EXPIRED | null

    @PositiveOrZero
    private int page = DEFAULT_PAGE;

    @Min(1)
    private int size = DEFAULT_SIZE;

    private String sort = DEFAULT_SORT;

    private String direction = DEFAULT_DIRECTION;

    public VoucherSearchCriteria normalize() {
        if (keyword != null) {
            String trimmed = keyword.trim();
            keyword = trimmed.isEmpty() ? null : trimmed;
        }
        if (status != null) {
            String norm = status.trim().toUpperCase();
            status = ALLOWED_STATUSES.contains(norm) ? norm : null;
        }
        if (page < 0) {
            page = DEFAULT_PAGE;
        }
        if (size < 1) {
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
