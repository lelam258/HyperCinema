package com.cinema.hyperCinema.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "hall_seat_type_price", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"hall_id", "seat_type"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HallSeatTypePrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hall_id", nullable = false)
    private Hall hall;

    @Column(name = "seat_type", nullable = false, length = 20)
    private String seatType;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Column(name = "active", nullable = false)
    private Boolean active = true;
}
