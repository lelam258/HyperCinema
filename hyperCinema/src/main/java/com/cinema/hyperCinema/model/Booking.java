package com.cinema.hyperCinema.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "booking")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id")
    private Integer bookingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "showtime_id")
    private Showtime showtime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    @Column(name = "total_price", nullable = false)
    private Long totalPrice;

    @Column(name = "seat_subtotal", nullable = false)
    private Long seatSubtotal = 0L;

    @Column(name = "food_subtotal", nullable = false)
    private Long foodSubtotal = 0L;

    @Column(name = "order_subtotal", nullable = false)
    private Long orderSubtotal = 0L;

    @Column(name = "voucher_discount_amount", nullable = false)
    private Long voucherDiscountAmount = 0L;

    @Column(name = "membership_discount_amount", nullable = false)
    private Long membershipDiscountAmount = 0L;

    @Column(name = "membership_plan_name", length = 100)
    private String membershipPlanName;

    @Column(name = "membership_discount_percent", precision = 5, scale = 2)
    private BigDecimal membershipDiscountPercent;

    @Column(length = 20)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
    private List<Ticket> tickets;

    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Payment payment;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
    private List<FoodOrder> foodOrders;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
    private List<PromotionUsage> promotionUsages;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

