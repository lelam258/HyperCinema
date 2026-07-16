package com.cinema.hyperCinema.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "FoodItem")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class FoodItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Integer itemId;

    @Column(name = "category_name", nullable = false, length = 100)
    private String categoryName;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false)
    private Integer price;

    @Column(nullable = false)
    private Integer stock;

    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @OneToMany(mappedBy = "foodItem", cascade = CascadeType.ALL)
    private List<FoodOrderItem> orderItems;

    @PrePersist
    protected void onCreate() {
        if (isAvailable == null) isAvailable = true;
        if (stock == null) stock = 0;
    }
}
