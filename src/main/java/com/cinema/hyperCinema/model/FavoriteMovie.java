package com.cinema.hyperCinema.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "favorite_movie")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteMovie {

    @EmbeddedId
    private FavoriteMovieId id;

    @Column(name = "favorited_at", nullable = false, updatable = false)
    private LocalDateTime favoritedAt;

    @PrePersist
    protected void onCreate() {
        if (favoritedAt == null) {
            favoritedAt = LocalDateTime.now();
        }
    }
}

