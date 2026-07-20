package com.cinema.hyperCinema.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "membership_plan")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class MembershipPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plan_id")
    private Integer planId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "discount_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @Column(nullable = false)
    private Integer price;

    @Column(nullable = false)
    private Integer level = 1;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays = 0;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL)
    private List<UserMembership> userMemberships;

    @PrePersist
    protected void onCreate() {
        if (durationDays == null) {
            durationDays = 0;
        }
        if (level == null || level < 1) {
            level = 1;
        }
        if (status == null || status.isBlank()) {
            status = "ACTIVE";
        }
    }
}

