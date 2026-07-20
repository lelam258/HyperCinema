package com.cinema.hyperCinema.dto.admin.membership.response;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MembershipPlanDetailView {
    private Integer planId;
    private String name;
    private BigDecimal discountPercent;
    private Integer price;
    private Integer level;
    private String status;
    private long activeUserCount;
    private boolean inUse;
}
