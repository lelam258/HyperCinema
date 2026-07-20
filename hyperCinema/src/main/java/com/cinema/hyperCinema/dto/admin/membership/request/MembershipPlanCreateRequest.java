package com.cinema.hyperCinema.dto.admin.membership.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MembershipPlanCreateRequest {

    @NotBlank(message = "{membership.plan.name.invalid}")
    @Size(max = 100, message = "{membership.plan.name.invalid}")
    private String name;

    @NotNull(message = "{membership.plan.discount.invalid}")
    @DecimalMin(value = "0.00", message = "{membership.plan.discount.invalid}")
    @DecimalMax(value = "100.00", message = "{membership.plan.discount.invalid}")
    private BigDecimal discountPercent;

    @NotNull(message = "{membership.plan.required_points.invalid}")
    @PositiveOrZero(message = "{membership.plan.required_points.invalid}")
    private Integer price;

    private Integer level = 1;

    private String status = "ACTIVE";
}
