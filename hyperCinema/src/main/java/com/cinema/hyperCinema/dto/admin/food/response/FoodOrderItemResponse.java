package com.cinema.hyperCinema.dto.admin.food.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodOrderItemResponse {

    private Integer itemId;
    private String itemName;
    private Integer quantity;
    private Integer unitPrice;
    private Integer subtotal;
}
