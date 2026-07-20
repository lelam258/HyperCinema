package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.MembershipPlan;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MembershipPlanRepository extends JpaRepository<MembershipPlan, Integer> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndPlanIdNot(String name, Integer planId);

    List<MembershipPlan> findByStatusIgnoreCaseOrderByNameAsc(String status);

    List<MembershipPlan> findByStatusIgnoreCaseOrderByLevelAscNameAsc(String status);

    List<MembershipPlan> findAllByOrderByLevelAscNameAsc();

    @Query("SELECT COALESCE(MAX(p.level), 0) FROM MembershipPlan p")
    int findMaxLevel();

    @Query("""
            SELECT p FROM MembershipPlan p
            WHERE (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:status IS NULL OR UPPER(p.status) = UPPER(:status))
            """)
    Page<MembershipPlan> searchPlans(@Param("keyword") String keyword,
                                     @Param("status") String status,
                                     Pageable pageable);
}
