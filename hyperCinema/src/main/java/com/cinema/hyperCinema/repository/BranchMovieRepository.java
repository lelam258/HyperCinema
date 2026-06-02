package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.BranchMovie;
import com.cinema.hyperCinema.model.BranchMovieId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BranchMovieRepository extends JpaRepository<BranchMovie, BranchMovieId> {

    @Query("SELECT bm FROM BranchMovie bm JOIN FETCH bm.branch b " +
           "WHERE bm.id.movieId = :movieId ORDER BY bm.assignedAt DESC")
    List<BranchMovie> findByMovieIdWithBranch(@Param("movieId") Integer movieId);

    @Query("SELECT (COUNT(bm) > 0) FROM BranchMovie bm WHERE bm.id.movieId = :movieId")
    boolean existsByMovie_MovieId(@Param("movieId") Integer movieId);

    boolean existsByIdBranchIdAndIdMovieId(Integer branchId, Integer movieId);
}
