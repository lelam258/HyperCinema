package com.cinema.hyperCinema.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class BranchMovieId implements Serializable {

    @Column(name = "branch_id")
    private Integer branchId;

    @Column(name = "movie_id")
    private Integer movieId;
}