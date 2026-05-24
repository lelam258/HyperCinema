package com.cinema.hyperCinema.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Ticket")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer ticketId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id")
    private Seat seat;

    @Column(name = "qr_code", length = 255)
    private String qrCode;

    @Column(length = 20)
    private String status;
}