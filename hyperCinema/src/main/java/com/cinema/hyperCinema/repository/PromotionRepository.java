package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.Promotion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository
        extends JpaRepository<Promotion, Integer>, JpaSpecificationExecutor<Promotion> {

    Optional<Promotion> findByCodeIgnoreCase(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Promotion p WHERE LOWER(p.code) = LOWER(:code)")
    Optional<Promotion> findByCodeIgnoreCaseForUpdate(@Param("code") String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndPromotionIdNot(String code, Integer promotionId);

    List<Promotion> findByStatusAndEndDateBefore(String status, LocalDateTime endDate);

    @Query("""
            SELECT p
            FROM Promotion p
            LEFT JOIN FETCH p.branch b
            WHERE p.status = :status
              AND p.startDate <= :now
              AND p.endDate >= :now
              AND p.usedCount < p.maxUses
              AND p.minOrderValue <= :orderValue
              AND (
                    p.branchSpecific = false
                    OR (:branchId IS NOT NULL AND b.branchId = :branchId)
                  )
            ORDER BY p.endDate ASC, p.createdAt DESC, p.title ASC
            """)
    List<Promotion> findAvailableVoucherCandidates(@Param("status") String status,
                                                    @Param("now") LocalDateTime now,
                                                    @Param("orderValue") long orderValue,
                                                    @Param("branchId") Integer branchId);
}

