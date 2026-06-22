package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BranchRepository
        extends JpaRepository<Branch, Integer>, JpaSpecificationExecutor<Branch> {

    boolean existsByCityIgnoreCaseAndNameIgnoreCase(String city, String name);

    boolean existsByCityIgnoreCaseAndNameIgnoreCaseAndBranchIdNot(
            String city, String name, Integer branchId);

    long countByStatus(String status);

    List<Branch> findAllByOrderByNameAsc();
}
