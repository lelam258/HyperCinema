package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.Hall;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HallRepository extends JpaRepository<Hall, Integer>, JpaSpecificationExecutor<Hall> {

    /**
     * Check whether at least one Hall is linked to the given branch.
     * Used by branch deletion guard (Requirement 5.5) and validation flows
     * that need to know if a branch currently has any halls (Requirement 4.2).
     */
    boolean existsByBranch_BranchId(Integer branchId);

    /**
     * Count halls belonging to a branch. Used by branch listing/detail views
     * to surface aggregate metrics (Requirements 4.2, 8.2).
     */
    long countByBranch_BranchId(Integer branchId);

    /**
     * Fetch all halls belonging to a branch. Used when displaying or operating
     * on the halls of a specific branch (Requirements 4.2, 8.2).
     */
    List<Hall> findByBranch_BranchId(Integer branchId);

    List<Hall> findByBranch_BranchIdAndStatusIgnoreCase(Integer branchId, String status);

    List<Hall> findByStatusIgnoreCaseOrderByNameAsc(String status);

    boolean existsByBranch_BranchIdAndNameIgnoreCase(Integer branchId, String name);

    boolean existsByBranch_BranchIdAndNameIgnoreCaseAndHallIdNot(
            Integer branchId, String name, Integer hallId);
}
