package com.cinema.hyperCinema.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.cinema.hyperCinema.model.Branch;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Integer> {
}
