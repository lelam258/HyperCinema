package com.cinema.hyperCinema.model;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class FoodOrderItemId implements Serializable {
    private Integer orderId;
    private Integer itemId;
}
