package com.cinema.hyperCinema.service.voucher;

import com.cinema.hyperCinema.dto.admin.voucher.response.VoucherPreview;
import com.cinema.hyperCinema.model.Booking;
import com.cinema.hyperCinema.model.Promotion;

import java.time.LocalDateTime;

import java.time.LocalDateTime;

/**
 * Service áp dụng voucher khi đặt vé.
 *
 * <p>Tách biệt khỏi quản trị voucher để khối đặt vé tái sử dụng. Phép tính giảm
 * giá được ủy quyền cho {@link VoucherCalculator} (hàm thuần). Xem design.md
 * section 6 "Service Interface: VoucherApplicationService".</p>
 */
public interface VoucherApplicationService {

    /**
     * Xác thực mã + xem trước mức giảm cho một đơn hàng. Không thay đổi trạng thái.
     *
     * @param code            mã voucher khách hàng nhập
     * @param orderValue      tổng giá trị đơn hàng trước giảm
     * @param bookingBranchId chi nhánh của đơn đặt vé (có thể null)
     * @param now             thời điểm hiện tại để kiểm tra thời hạn hiệu lực
     * @return {@link VoucherPreview} với {@code valid = true} kèm mức giảm khi hợp lệ,
     *         hoặc {@code valid = false} kèm {@code errorKey} tương ứng khi không hợp lệ
     */
    VoucherPreview validateAndPreview(String code, long orderValue, Integer bookingBranchId, LocalDateTime now);

    /**
     * Áp dụng voucher đã xác thực vào booking: tăng {@code usedCount} và ghi
     * {@link com.cinema.hyperCinema.model.PromotionUsage}.
     *
     * @param code    mã voucher cần áp dụng
     * @param booking đơn đặt vé được áp dụng voucher
     * @param now     thời điểm áp dụng
     */
    void apply(String code, Booking booking, LocalDateTime now);

    Promotion applyValidated(String code, Booking booking, long orderValue, Integer bookingBranchId, LocalDateTime now);
}
