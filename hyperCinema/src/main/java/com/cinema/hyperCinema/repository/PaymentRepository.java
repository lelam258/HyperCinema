package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    @Query("SELECT SUM(p.amount) FROM Payment p "
            + "JOIN p.booking bk JOIN bk.showtime s JOIN s.hall h "
            + "WHERE h.branch.branchId = :branchId "
            + "AND p.createdAt BETWEEN :start AND :end "
            + "AND p.status = 'Completed'")
    Long sumRevenueByBranchAndDateRange(@Param("branchId") Integer branchId,
                                        @Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end);


}
