package com.cinema.hyperCinema.dto.admin.food.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FoodOrderCreateRequest {

    @NotNull(message = "{food.order.booking.not_found}")
    private Integer bookingId;

    @NotEmpty(message = "{food.order.quantity.invalid}")
    @Valid
    private List<FoodOrderItemRequest> items;
}
