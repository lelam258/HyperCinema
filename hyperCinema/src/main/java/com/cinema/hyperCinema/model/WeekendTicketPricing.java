package com.cinema.hyperCinema.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "weekend_ticket_pricing", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"hall_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WeekendTicketPricing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hall_id", nullable = false)
    private Hall hall;

    @Column(name = "name", nullable = false, length = 100)
    private String name = "Weekend ticket pricing";

    @Column(name = "days_of_week", nullable = false, length = 80)
    private String daysOfWeek = "SATURDAY,SUNDAY";

    @Column(name = "standard_price", nullable = false)
    private Integer standardPrice = 0;

    @Column(name = "vip_price", nullable = false)
    private Integer vipPrice = 0;

    @Column(name = "couple_price", nullable = false)
    private Integer couplePrice = 0;

    @Column(name = "disabled_price", nullable = false)
    private Integer disabledPrice = 0;

    @Column(name = "active", nullable = false)
    private Boolean active = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
