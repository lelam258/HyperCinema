package com.cinema.hyperCinema.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Favorite_Movie")
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
