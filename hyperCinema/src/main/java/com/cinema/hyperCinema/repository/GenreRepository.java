package com.cinema.hyperCinema.repository;

import java.util.Collection;
import java.util.List;

import com.cinema.hyperCinema.model.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GenreRepository extends JpaRepository<Genre, Integer> {

    long countByGenreIdIn(Collection<Integer> genreIds);

    List<Genre> findAllByOrderByNameAsc();
}
