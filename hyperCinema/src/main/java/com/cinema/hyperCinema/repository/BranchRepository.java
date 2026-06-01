package com.cinema.hyperCinema.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cinema.hyperCinema.model.Branch;

@Repository
public interface BranchRepository
        extends JpaRepository<Branch, Integer>, JpaSpecificationExecutor<Branch> {

    boolean existsByCityIgnoreCaseAndNameIgnoreCase(String city, String name);

    boolean existsByCityIgnoreCaseAndNameIgnoreCaseAndBranchIdNot(
            String city, String name, Integer branchId);

    long countByStatus(String status);
}
