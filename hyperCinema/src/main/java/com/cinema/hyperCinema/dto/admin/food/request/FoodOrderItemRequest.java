package com.cinema.hyperCinema.dto.admin.food.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FoodOrderItemRequest {

    @NotNull
    private Integer itemId;

    @NotNull(message = "{food.order.quantity.invalid}")
    @Min(value = 1, message = "{food.order.quantity.invalid}")
    private Integer quantity;
}
