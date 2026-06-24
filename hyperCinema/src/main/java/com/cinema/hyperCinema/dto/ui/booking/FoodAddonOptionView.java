package com.cinema.hyperCinema.dto.ui.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
