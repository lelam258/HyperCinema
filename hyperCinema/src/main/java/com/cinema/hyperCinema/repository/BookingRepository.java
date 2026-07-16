package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    @EntityGraph(attributePaths = {
            "showtime",
            "showtime.movie",
            "showtime.hall",
            "showtime.hall.branch",
            "tickets",
            "tickets.seat"
    })
    List<Booking> findByUser_UserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    long countByShowtime_ShowtimeId(Integer showtimeId);

    @Query(value = """
            SELECT b
            FROM Booking b
            LEFT JOIN FETCH b.user u
            LEFT JOIN FETCH b.showtime s
            LEFT JOIN FETCH s.movie m
            LEFT JOIN FETCH s.hall h
            LEFT JOIN FETCH h.branch br
            LEFT JOIN FETCH b.payment p
            WHERE br.branchId = :branchId
            AND (:bookingStatus IS NULL OR b.status = :bookingStatus)
            AND (:paymentStatus IS NULL OR p.status = :paymentStatus)
            AND (:movieId IS NULL OR m.movieId = :movieId)
            AND (:createdFrom IS NULL OR b.createdAt >= :createdFrom)
            AND (:createdTo IS NULL OR b.createdAt < :createdTo)
            AND (:showtimeFrom IS NULL OR s.startTime >= :showtimeFrom)
            AND (:showtimeTo IS NULL OR s.startTime < :showtimeTo)
            AND (
                :keyword IS NULL
                OR LOWER(CONCAT('', b.bookingId)) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(u.fullName, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(u.username, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(u.email, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(m.title, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            """,
            countQuery = """
            SELECT COUNT(b)
            FROM Booking b
            LEFT JOIN b.user u
            LEFT JOIN b.showtime s
            LEFT JOIN s.movie m
            LEFT JOIN s.hall h
            LEFT JOIN h.branch br
            LEFT JOIN b.payment p
            WHERE br.branchId = :branchId
            AND (:bookingStatus IS NULL OR b.status = :bookingStatus)
            AND (:paymentStatus IS NULL OR p.status = :paymentStatus)
            AND (:movieId IS NULL OR m.movieId = :movieId)
            AND (:createdFrom IS NULL OR b.createdAt >= :createdFrom)
            AND (:createdTo IS NULL OR b.createdAt < :createdTo)
            AND (:showtimeFrom IS NULL OR s.startTime >= :showtimeFrom)
            AND (:showtimeTo IS NULL OR s.startTime < :showtimeTo)
            AND (
                :keyword IS NULL
                OR LOWER(CONCAT('', b.bookingId)) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(u.fullName, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(u.username, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(u.email, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(m.title, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            """)
    Page<Booking> searchForBranchManagement(@Param("branchId") Integer branchId,
                                            @Param("keyword") String keyword,
                                            @Param("bookingStatus") String bookingStatus,
                                            @Param("paymentStatus") String paymentStatus,
                                            @Param("movieId") Integer movieId,
                                            @Param("createdFrom") LocalDateTime createdFrom,
                                            @Param("createdTo") LocalDateTime createdTo,
                                            @Param("showtimeFrom") LocalDateTime showtimeFrom,
                                            @Param("showtimeTo") LocalDateTime showtimeTo,
                                            Pageable pageable);

    @Query("""
            SELECT b
            FROM Booking b
            LEFT JOIN FETCH b.user u
            LEFT JOIN FETCH b.showtime s
            LEFT JOIN FETCH s.movie m
            LEFT JOIN FETCH s.hall h
            LEFT JOIN FETCH h.branch br
            LEFT JOIN FETCH b.payment p
            WHERE b.bookingId = :bookingId
            """)
    Optional<Booking> findManagementDetailById(@Param("bookingId") Integer bookingId);
}
