package com.cinema.hyperCinema.dto.admin.hall.request;

import java.util.Set;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;

public class HallSearchCriteria {

    public static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("hallId", "name", "hallType", "capacity", "status");

    public static final Set<String> ALLOWED_DIRECTIONS =
            Set.of("ASC", "DESC");

    public static final String DEFAULT_SORT = "hallId";

    public static final String DEFAULT_DIRECTION = "ASC";

    public static final int DEFAULT_PAGE = 0;

    public static final int DEFAULT_SIZE = 10;

    private String keyword;

    private Integer branchId;

    private String city;

    private String hallType;

    private String status;

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
        this.keyword = keyword;
    }

    public Integer getBranchId() {
        return branchId;
    }

    public void setBranchId(Integer branchId) {
        this.branchId = branchId;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getHallType() {
        return hallType;
    }

    public void setHallType(String hallType) {
        this.hallType = hallType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
        this.size = (size == null || size < 1) ? DEFAULT_SIZE : size;
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

    public HallSearchCriteria normalize() {
        setPage(this.page);
        setSize(this.size);
        setSort(this.sort);
        setDirection(this.direction);
        return this;
    }
}
