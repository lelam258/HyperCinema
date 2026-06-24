package com.cinema.hyperCinema.service.voucher;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.hyperCinema.dto.admin.voucher.response.VoucherPreview;
import com.cinema.hyperCinema.model.Booking;
import com.cinema.hyperCinema.model.Promotion;
import com.cinema.hyperCinema.model.PromotionUsage;
import com.cinema.hyperCinema.repository.PromotionRepository;
import com.cinema.hyperCinema.repository.PromotionUsageRepository;

import lombok.RequiredArgsConstructor;

/**
 * Triển khai {@link VoucherApplicationService}.
 *
 * <p>{@code validateAndPreview} là hàm chỉ-đọc (không thay đổi trạng thái) kiểm tra
 * đầy đủ điều kiện áp dụng theo mục "Voucher Applicability" của design.md.
 * {@code apply} tăng {@code usedCount} và ghi {@link PromotionUsage} trong một
 * giao dịch.</p>
 */
@Service
@RequiredArgsConstructor
public class VoucherApplicationServiceImpl implements VoucherApplicationService {

    private static final String STATUS_ACTIVE = "ACTIVE";

    private final PromotionRepository promotionRepository;
    private final PromotionUsageRepository promotionUsageRepository;

    @Override
    @Transactional(readOnly = true)
    public VoucherPreview validateAndPreview(String code, long orderValue, Integer bookingBranchId, LocalDateTime now) {
        // Req 5.2: voucher tồn tại
        Promotion voucher = promotionRepository.findByCodeIgnoreCase(code).orElse(null);
        if (voucher == null) {
            return invalid(code, "voucher.apply.not_found");
        }

        // Req 5.3, 6.2: chỉ ACTIVE (INACTIVE/EXPIRED bị từ chối)
        if (!STATUS_ACTIVE.equals(voucher.getStatus())) {
            return invalid(code, "voucher.apply.inactive");
        }

        // Req 5.5: trong khoảng [startDate, endDate]
        if (now.isBefore(voucher.getStartDate()) || now.isAfter(voucher.getEndDate())) {
            return invalid(code, "voucher.apply.out_of_period");
        }

        // Req 5.4: chưa hết lượt sử dụng
        if (voucher.getUsedCount() >= voucher.getMaxUses()) {
            return invalid(code, "voucher.apply.exhausted");
        }

        // Req 5.6: đạt giá trị đơn hàng tối thiểu
        if (orderValue < voucher.getMinOrderValue()) {
            return invalid(code, "voucher.apply.below_min_order");
        }

        // Req 5.7: voucher theo chi nhánh phải khớp chi nhánh đơn đặt vé
        if (Boolean.TRUE.equals(voucher.getBranchSpecific())) {
            Integer voucherBranchId = voucher.getBranch() != null ? voucher.getBranch().getBranchId() : null;
            if (!Objects.equals(voucherBranchId, bookingBranchId)) {
                return invalid(code, "voucher.apply.branch_mismatch");
            }
        }

        // Req 5.9, 5.10, 5.11: tính mức giảm/giá cuối qua VoucherCalculator (đã kẹp [0, orderValue])
        long discountAmount = VoucherCalculator.discountFor(
                voucher.getDiscountType(), voucher.getDiscountValue(), orderValue);
        long finalPrice = VoucherCalculator.finalPrice(
                voucher.getDiscountType(), voucher.getDiscountValue(), orderValue);

        return VoucherPreview.builder()
                .valid(true)
                .code(voucher.getCode())
                .discountAmount(discountAmount)
                .finalPrice(finalPrice)
                .errorKey(null)
                .build();
    }

    @Override
    @Transactional
    public void apply(String code, Booking booking, LocalDateTime now) {
        // Req 5.8: nạp voucher, tăng usedCount và ghi PromotionUsage
        Promotion voucher = promotionRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new IllegalArgumentException("Voucher not found: " + code));

        voucher.setUsedCount(voucher.getUsedCount() + 1);

        PromotionUsage usage = new PromotionUsage();
        usage.setPromotion(voucher);
        usage.setBooking(booking);
        usage.setUser(booking != null ? booking.getUser() : null);
        usage.setUsedAt(now);

        promotionUsageRepository.save(usage);
        promotionRepository.save(voucher);
    }

    private VoucherPreview invalid(String code, String errorKey) {
        return VoucherPreview.builder()
                .valid(false)
                .code(code)
                .discountAmount(0L)
                .finalPrice(0L)
                .errorKey(errorKey)
                .build();
    }
}
