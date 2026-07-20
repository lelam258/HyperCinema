package com.cinema.hyperCinema.dto.admin.membership.response;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserMembershipListItem {
    private Integer membershipId;
    private Integer userId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private Integer planId;
    private String planName;
    private BigDecimal discountPercent;
    private String status;
}
