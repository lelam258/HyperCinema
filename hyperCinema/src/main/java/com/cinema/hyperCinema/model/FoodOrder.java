package com.cinema.hyperCinema.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Food_Order")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class FoodOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer foodOrderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Column(name = "total_price", nullable = false)
    private Long totalPrice;

    @Column(length = 20)
    private String status;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "foodOrder", cascade = CascadeType.ALL)
    private List<FoodOrderItem> items;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
