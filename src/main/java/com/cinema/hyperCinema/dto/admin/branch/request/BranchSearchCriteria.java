package com.cinema.hyperCinema.dto.admin.branch.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.Set;

public class BranchSearchCriteria {

    public static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("name", "city", "status", "createdAt");

    public static final Set<String> ALLOWED_DIRECTIONS =
            Set.of("ASC", "DESC");

    public static final String DEFAULT_SORT = "createdAt";

    public static final String DEFAULT_DIRECTION = "DESC";

    public static final int DEFAULT_PAGE = 0;

    public static final int DEFAULT_SIZE = 10;

    private String keyword;

    private String city;

    private String status;

    @PositiveOrZero(message = "page phải >= 0")
    private Integer page = DEFAULT_PAGE;

    @Min(value = 1, message = "size phải >= 1")
    private Integer size = DEFAULT_SIZE;

    private String sort = DEFAULT_SORT;

    private String direction = DEFAULT_DIRECTION;

    public BranchSearchCriteria() {
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
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
        if (sort == null || sort.isBlank() || !ALLOWED_SORT_FIELDS.contains(sort)) {
            this.sort = DEFAULT_SORT;
        } else {
            this.sort = sort;
        }
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

    public BranchSearchCriteria normalize() {
        setPage(this.page);
        setSize(this.size);
        setSort(this.sort);
        setDirection(this.direction);
        return this;
    }
}
