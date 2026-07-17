package com.cinema.hyperCinema.dto.admin.showtime.request;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;

public class ShowtimeSearchCriteria {

    public static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("showtimeId", "startTime", "endTime", "movieTitle", "branchName", "hallName", "price");

    public static final Map<String, String> SORT_PROPERTIES = Map.of(
            "showtimeId", "showtimeId",
            "startTime", "startTime",
            "endTime", "endTime",
            "movieTitle", "movie.title",
            "branchName", "hall.branch.name",
            "hallName", "hall.name",
            "price", "price");

    public static final Set<String> ALLOWED_DIRECTIONS =
            Set.of("ASC", "DESC");

    public static final String DEFAULT_SORT = "startTime";

    public static final String DEFAULT_DIRECTION = "ASC";

    public static final int DEFAULT_PAGE = 0;

    public static final int DEFAULT_SIZE = 10;

    public static final List<Integer> ALLOWED_PAGE_SIZES = List.of(10, 20, 50);

    private String keyword;

    private Integer movieId;

    private Integer branchId;

    private Integer hallId;

    private LocalDate dateFrom;

    private LocalDate dateTo;

    private String timeState = "UPCOMING";

    @PositiveOrZero(message = "page must be >= 0")
    private Integer page = DEFAULT_PAGE;

    @Min(value = 1, message = "size must be >= 1")
    private Integer size = DEFAULT_SIZE;

    private String sort = DEFAULT_SORT;

    private String direction = DEFAULT_DIRECTION;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = normalizeKeyword(keyword);
    }

    public Integer getMovieId() {
        return movieId;
    }

    public void setMovieId(Integer movieId) {
        this.movieId = movieId;
    }

    public Integer getBranchId() {
        return branchId;
    }

    public void setBranchId(Integer branchId) {
        this.branchId = branchId;
    }

    public Integer getHallId() {
        return hallId;
    }

    public void setHallId(Integer hallId) {
        this.hallId = hallId;
    }

    public LocalDate getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(LocalDate dateFrom) {
        this.dateFrom = dateFrom;
    }

    public LocalDate getDateTo() {
        return dateTo;
    }

    public void setDateTo(LocalDate dateTo) {
        this.dateTo = dateTo;
    }

    public String getTimeState() {
        return timeState;
    }

    public void setTimeState(String timeState) {
        if (timeState == null || timeState.isBlank()) {
            this.timeState = "ALL";
            return;
        }
        String normalized = timeState.trim().toUpperCase();
        this.timeState = Set.of("ALL", "UPCOMING", "PAST").contains(normalized)
                ? normalized
                : "ALL";
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = (page == null || page < 0) ? DEFAULT_PAGE : page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = (size == null || !ALLOWED_PAGE_SIZES.contains(size)) ? DEFAULT_SIZE : size;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = (sort == null || sort.isBlank() || !ALLOWED_SORT_FIELDS.contains(sort))
                ? DEFAULT_SORT
                : sort;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        if (direction == null || direction.isBlank()) {
            this.direction = DEFAULT_DIRECTION;
            return;
        }
        String normalized = direction.trim().toUpperCase();
        this.direction = ALLOWED_DIRECTIONS.contains(normalized)
                ? normalized
                : DEFAULT_DIRECTION;
    }

    public ShowtimeSearchCriteria normalize() {
        setKeyword(this.keyword);
        setPage(this.page);
        setSize(this.size);
        setSort(this.sort);
        setDirection(this.direction);
        setTimeState(this.timeState);
        return this;
    }

    public String toSortProperty() {
        return SORT_PROPERTIES.getOrDefault(sort, DEFAULT_SORT);
    }

    private static String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
