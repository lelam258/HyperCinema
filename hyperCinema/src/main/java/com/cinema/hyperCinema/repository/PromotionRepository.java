package com.cinema.hyperCinema.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cinema.hyperCinema.model.Promotion;

@Repository
public interface PromotionRepository
        extends JpaRepository<Promotion, Integer>, JpaSpecificationExecutor<Promotion> {

    /** Tìm voucher theo Code (không phân biệt hoa thường). */
    Optional<Promotion> findByCodeIgnoreCase(String code);

    /** Kiểm tra tồn tại Code (không phân biệt hoa thường) — phục vụ kiểm tra trùng khi tạo. */
    boolean existsByCodeIgnoreCase(String code);

    /** Trùng Code khi cập nhật (loại trừ chính voucher đang sửa). */
    boolean existsByCodeIgnoreCaseAndPromotionIdNot(String code, Integer promotionId);

    /** Tự động hết hạn: các voucher có trạng thái cho trước và đã quá end_date. */
    List<Promotion> findByStatusAndEndDateBefore(String status, LocalDateTime now);
}
