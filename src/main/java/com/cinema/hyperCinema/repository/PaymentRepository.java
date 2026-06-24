package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    Optional<Payment> findByBooking_BookingId(Integer bookingId);

    @Query("SELECT SUM(p.amount) FROM Payment p "
            + "WHERE p.createdAt BETWEEN :start AND :end "
            + "AND p.status = :status")
    Long sumAmountByCreatedAtBetweenAndStatus(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("status") String status);

    /**
     * Doanh thu theo chi nhánh (Leaderboard).
     * Returns: [branchName, revenue]
     */
    @Query("SELECT b.name, SUM(p.amount) "
            + "FROM Payment p JOIN p.booking bk JOIN bk.showtime s JOIN s.hall h JOIN h.branch b "
            + "WHERE p.status = 'Completed' AND p.createdAt BETWEEN :start AND :end "
            + "GROUP BY b.branchId, b.name ORDER BY SUM(p.amount) DESC")
    List<Object[]> findBranchRevenueLeaderboard(@Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end);

    @Query("SELECT SUM(p.amount) FROM Payment p "
            + "JOIN p.booking bk JOIN bk.showtime s JOIN s.hall h "
            + "WHERE h.branch.branchId = :branchId "
            + "AND p.createdAt BETWEEN :start AND :end "
            + "AND p.status = 'Completed'")
    Long sumRevenueByBranchAndDateRange(@Param("branchId") Integer branchId,
                                        @Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end);

    long countByBooking_Showtime_ShowtimeId(Integer showtimeId);
}
