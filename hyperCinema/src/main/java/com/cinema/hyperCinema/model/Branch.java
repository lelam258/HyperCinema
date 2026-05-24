package com.cinema.hyperCinema.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "Branch")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer branchId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String location;

    @OneToMany(mappedBy = "branch", cascade = CascadeType.ALL)
    private List<Hall> halls;
}
