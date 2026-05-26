package com.cinema.hyperCinema.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "Membership_Plan")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class MembershipPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer planId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "discount_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @Column(nullable = false)
    private Integer price;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL)
    private List<UserMembership> userMemberships;
}
