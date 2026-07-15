package com.cinema.hyperCinema.dto.admin.food.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FoodItemCreateRequest {

    @NotBlank(message = "{food.item.name.invalid}")
    @Size(max = 149, message = "{food.item.name.invalid}")
    private String name;

    @NotBlank(message = "{food.item.category.invalid}")
    @Size(max = 100, message = "{food.item.category.invalid}")
    private String categoryName;

    @Size(max = 500, message = "{food.item.description.invalid}")
    private String description;

    @NotNull(message = "{food.item.price.invalid}")
    @Min(value = 1, message = "{food.item.price.invalid}")
    private Integer price;

    @NotNull(message = "{food.item.stock.invalid_create}")
    @Min(value = 1, message = "{food.item.stock.invalid_create}")
    private Integer stock;

    private String imageUrl;
}
