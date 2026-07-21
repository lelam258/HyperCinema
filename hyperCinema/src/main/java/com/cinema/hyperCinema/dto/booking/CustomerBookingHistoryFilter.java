package com.cinema.hyperCinema.dto.booking;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerBookingHistoryFilter {

    public static final int DEFAULT_SIZE = 10;
    public static final String DEFAULT_SORT = "createdAt";
    public static final String DEFAULT_DIRECTION = "desc";

    private static final Set<String> ALLOWED_DIRECTIONS = Set.of("asc", "desc");

    private String keyword;
    private String bookingStatus;
    private String paymentStatus;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate showtimeFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate showtimeTo;

    private String sort = DEFAULT_SORT;
    private String direction = DEFAULT_DIRECTION;
    private Integer size = DEFAULT_SIZE;

    public void normalize() {
        sort = normalizeSort(sort);
        direction = normalizeDirection(direction);
        size = normalizeSize(size);
    }

    public String sortField() {
        return switch (normalizeSort(sort)) {
            case "showtime" -> "showtime.startTime";
            case "movie" -> "showtime.movie.title";
            case "totalPrice" -> "totalPrice";
            case "status" -> "status";
            case "bookingId" -> "bookingId";
            default -> "createdAt";
        };
    }

    public Sort.Direction sortDirection() {
        return "asc".equalsIgnoreCase(normalizeDirection(direction))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
    }

    public Sort toSort() {
        String field = sortField();
        Sort.Direction sortDirection = sortDirection();
        Sort primary = Sort.by(sortDirection, field);
        if ("bookingId".equals(field)) {
            return primary;
        }
        Sort.Direction tieBreakerDirection = "createdAt".equals(field) ? sortDirection : Sort.Direction.DESC;
        return primary.and(Sort.by(tieBreakerDirection, "bookingId"));
    }

    public LocalDateTime createdFromStart() {
        return startOfDay(createdFrom);
    }

    public LocalDateTime createdToExclusive() {
        return dayAfter(createdTo);
    }

    public LocalDateTime showtimeFromStart() {
        return startOfDay(showtimeFrom);
    }

    public LocalDateTime showtimeToExclusive() {
        return dayAfter(showtimeTo);
    }

    public int pageSize() {
        return normalizeSize(size);
    }

    private static String normalizeSort(String value) {
        if (value == null) {
            return DEFAULT_SORT;
        }
        return switch (value.trim()) {
            case "createdAt", "showtime", "showtime.startTime", "movie", "showtime.movie.title",
                 "totalPrice", "status", "bookingId" -> canonicalSort(value.trim());
            default -> DEFAULT_SORT;
        };
    }

    private static String canonicalSort(String value) {
        return switch (value) {
            case "showtime.startTime" -> "showtime";
            case "showtime.movie.title" -> "movie";
            default -> value;
        };
    }

    private static String normalizeDirection(String value) {
        if (value == null) {
            return DEFAULT_DIRECTION;
        }
        String normalized = value.trim().toLowerCase();
        return ALLOWED_DIRECTIONS.contains(normalized) ? normalized : DEFAULT_DIRECTION;
    }

    private static int normalizeSize(Integer value) {
        if (value == null || value < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(value, 50);
    }

    private static LocalDateTime startOfDay(LocalDate date) {
        return date != null ? date.atStartOfDay() : null;
    }

    private static LocalDateTime dayAfter(LocalDate date) {
        return date != null ? date.plusDays(1).atStartOfDay() : null;
    }
}
