package com.cinema.hyperCinema.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Language")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Language {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "language_id")
    private Integer languageId;

    @Column(nullable = false, unique = true, length = 50)
    private String name;
}
