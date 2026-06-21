package com.cinema.hyperCinema.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Promotion")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer promotionId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "discount_type", nullable = false, length = 50)
    private String discountType;          // PERCENTAGE | FIXED_AMOUNT

    @Column(name = "discount_value", nullable = false)
    private Integer discountValue;

    @Column(name = "min_order_value", nullable = false)
    private Integer minOrderValue;        // >= 0, default 0

    @Column(name = "max_uses", nullable = false)
    private Integer maxUses;              // >= 1

    @Column(name = "used_count", nullable = false)
    private Integer usedCount;            // default 0

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "is_branch_specific", nullable = false)
    private Boolean branchSpecific;       // default false

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;                // null nếu áp dụng toàn hệ thống

    @Column(nullable = false, length = 50)
    private String status;                // ACTIVE | INACTIVE | EXPIRED

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "promotion")
    private List<Booking> bookings;

    @OneToMany(mappedBy = "promotion", cascade = CascadeType.ALL)
    private List<PromotionUsage> usages;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (usedCount == null) usedCount = 0;
        if (minOrderValue == null) minOrderValue = 0;
        if (branchSpecific == null) branchSpecific = false;
        if (status == null) status = "ACTIVE";
    }
}
