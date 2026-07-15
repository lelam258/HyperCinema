package com.cinema.hyperCinema.dto.admin.food.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodItemDetailResponse {

    private Integer itemId;
    private String name;
    private String categoryName;
    private String description;
    private Integer price;
    private Integer stock;
    private Boolean isAvailable;
    private String imageUrl;
}
