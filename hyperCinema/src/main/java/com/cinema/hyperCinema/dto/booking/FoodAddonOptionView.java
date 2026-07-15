package com.cinema.hyperCinema.dto.booking;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodAddonOptionView {

    private Integer itemId;

    private String name;

    private String categoryName;

    private Integer price;

    private String displayPrice;

    private Integer stock;

    private boolean available;

    private String unavailableReason;
}
