package com.cinema.hyperCinema.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "Food_Item")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class FoodItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer foodId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Integer price;

    @Column(length = 50)
    private String category;

    @Column(length = 20)
    private String status;

    @OneToMany(mappedBy = "food", cascade = CascadeType.ALL)
    private List<FoodOrderItem> orderItems;
}
