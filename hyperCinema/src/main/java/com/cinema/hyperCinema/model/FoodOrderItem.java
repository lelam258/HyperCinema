package com.cinema.hyperCinema.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "food_order_item")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@IdClass(FoodOrderItemId.class)
public class FoodOrderItem {

    @Id
    @Column(name = "food_order_id")
    private Integer orderId;

    @Id
    @Column(name = "food_id")
    private Integer itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_order_id", insertable = false, updatable = false)
    private FoodOrder foodOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_id", insertable = false, updatable = false)
    private FoodItem foodItem;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false)
    private Integer unitPrice;
}

