package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BranchRepository
        extends JpaRepository<Branch, Integer>, JpaSpecificationExecutor<Branch> {

    boolean existsByCityIgnoreCaseAndNameIgnoreCase(String city, String name);

    boolean existsByCityIgnoreCaseAndNameIgnoreCaseAndBranchIdNot(
            String city, String name, Integer branchId);

    long countByStatus(String status);

    @Query("SELECT b FROM Branch b "
            + "WHERE NOT EXISTS ("
            + "SELECT 1 FROM BranchMovie bm "
            + "WHERE bm.branch = b AND bm.movie.movieId = :movieId"
            + ") ORDER BY b.name ASC")
    List<Branch> findBranchesNotAssignedToMovie(@Param("movieId") Integer movieId);
}
