package com.cinema.hyperCinema.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Seat")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer seatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hall_id")
    private Hall hall;

    @Column(name = "seat_row", nullable = false, length = 5)
    private String seatRow;

    @Column(name = "seat_number", nullable = false)
    private Integer seatNumber;

    @Column(length = 20)
    private String type;
}
