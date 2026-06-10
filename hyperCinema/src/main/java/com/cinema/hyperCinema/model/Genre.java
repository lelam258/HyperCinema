package com.cinema.hyperCinema.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Genre")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer genreId;

    @Column(nullable = false, unique = true, length = 50)
    private String name;
}
