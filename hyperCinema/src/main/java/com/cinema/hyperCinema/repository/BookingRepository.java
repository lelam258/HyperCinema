package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.Booking;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Integer> {

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Kiểm tra Active_Booking_Reference: tồn tại Booking tham chiếu voucher
     * (promotionId) với status khác giá trị truyền vào (thường là "CANCELLED").
     * Dùng để chặn xóa voucher đang được sử dụng (Requirement 4.1, 4.2).
     */
    boolean existsByPromotion_PromotionIdAndStatusNot(Integer promotionId, String status);

    /**
     * Top N phim theo số booking (không tính Cancelled).
     * Returns: [movieTitle, bookingCount]
     */
    default List<Object[]> findTopMoviesByBookingCount(int limit) {
        return findTopMoviesByBookingCount(PageRequest.of(0, limit));
    }

    @Query("SELECT m.title, COUNT(b) AS cnt "
            + "FROM Booking b JOIN b.showtime s JOIN s.movie m "
            + "WHERE b.status <> 'Cancelled' "
            + "GROUP BY m.title ORDER BY cnt DESC")
    List<Object[]> findTopMoviesByBookingCount(Pageable pageable);

    /**
     * Tổng số vé bán (không tính Cancelled).
     */
    @Query("SELECT COUNT(t) FROM Ticket t JOIN t.booking b "
            + "WHERE b.status <> 'Cancelled' AND b.createdAt BETWEEN :start AND :end")
    long countTicketsByBookingStatusAndCreatedAtBetween(@Param("start") LocalDateTime start,
                                                        @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(b) FROM Booking b JOIN b.showtime s JOIN s.hall h "
            + "WHERE h.branch.branchId = :branchId "
            + "AND b.createdAt BETWEEN :start AND :end")
    long countBookingsByBranchIdAndDateRange(@Param("branchId") Integer branchId,
                                             @Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(t) FROM Ticket t JOIN t.booking b JOIN b.showtime s JOIN s.hall h "
            + "WHERE h.branch.branchId = :branchId "
            + "AND b.status <> 'Cancelled' "
            + "AND b.createdAt BETWEEN :start AND :end")
    long countTicketsByBranchIdAndDateRange(@Param("branchId") Integer branchId,
                                            @Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);

    default List<Object[]> findTopMoviesByBranchId(Integer branchId, int limit) {
        return findTopMoviesByBranchId(branchId, PageRequest.of(0, limit));
    }

    @Query("SELECT m.title, COUNT(b) AS cnt "
            + "FROM Booking b JOIN b.showtime s JOIN s.movie m JOIN s.hall h "
            + "WHERE h.branch.branchId = :branchId "
            + "AND b.status <> 'Cancelled' "
            + "GROUP BY m.title ORDER BY cnt DESC")
    List<Object[]> findTopMoviesByBranchId(@Param("branchId") Integer branchId,
                                           Pageable pageable);

    long countByUser_UserId(Integer userId);

    List<Booking> findByUser_UserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    long countByShowtime_ShowtimeId(Integer showtimeId);
}
