package com.cinema.hyperCinema.service.voucher;

import com.cinema.hyperCinema.dto.admin.voucher.response.VoucherPreview;
import com.cinema.hyperCinema.model.Booking;
import com.cinema.hyperCinema.model.Promotion;
import com.cinema.hyperCinema.model.PromotionUsage;
import com.cinema.hyperCinema.repository.PromotionRepository;
import com.cinema.hyperCinema.repository.PromotionUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class VoucherApplicationServiceImpl implements VoucherApplicationService {

    private static final String STATUS_ACTIVE = "ACTIVE";

    private final PromotionRepository promotionRepository;
    private final PromotionUsageRepository promotionUsageRepository;

    @Override
    @Transactional(readOnly = true)
    public VoucherPreview validateAndPreview(String code, long orderValue, Integer bookingBranchId, LocalDateTime now) {
        Promotion voucher = promotionRepository.findByCodeIgnoreCase(code).orElse(null);
        return validate(voucher, code, orderValue, bookingBranchId, now);
    }

    @Override
    @Transactional
    public void apply(String code, Booking booking, LocalDateTime now) {
        Promotion voucher = promotionRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new IllegalArgumentException("Voucher not found: " + code));
        recordUsage(voucher, booking, now);
    }

    @Override
    @Transactional
    public Promotion applyValidated(String code, Booking booking, long orderValue, Integer bookingBranchId, LocalDateTime now) {
        Promotion voucher = promotionRepository.findByCodeIgnoreCaseForUpdate(code)
                .orElseThrow(() -> new IllegalArgumentException("voucher.apply.not_found"));
        VoucherPreview preview = validate(voucher, code, orderValue, bookingBranchId, now);
        if (!preview.isValid()) {
            throw new IllegalArgumentException(preview.getErrorKey());
        }
        recordUsage(voucher, booking, now);
        return voucher;
    }

    private void recordUsage(Promotion voucher, Booking booking, LocalDateTime now) {
        voucher.setUsedCount(voucher.getUsedCount() + 1);

        PromotionUsage usage = new PromotionUsage();
        usage.setPromotion(voucher);
        usage.setBooking(booking);
        usage.setUser(booking != null ? booking.getUser() : null);
        usage.setUsedAt(now);

        promotionUsageRepository.save(usage);
        promotionRepository.save(voucher);
    }

    private VoucherPreview validate(Promotion voucher, String code, long orderValue, Integer bookingBranchId, LocalDateTime now) {
        if (voucher == null) {
            return invalid(code, "voucher.apply.not_found");
        }
        if (!STATUS_ACTIVE.equals(voucher.getStatus())) {
            return invalid(code, "voucher.apply.inactive");
        }
        if (now.isBefore(voucher.getStartDate()) || now.isAfter(voucher.getEndDate())) {
            return invalid(code, "voucher.apply.out_of_period");
        }
        if (voucher.getUsedCount() >= voucher.getMaxUses()) {
            return invalid(code, "voucher.apply.exhausted");
        }
        if (orderValue < voucher.getMinOrderValue()) {
            return invalid(code, "voucher.apply.below_min_order");
        }
        if (Boolean.TRUE.equals(voucher.getBranchSpecific())) {
            Integer voucherBranchId = voucher.getBranch() != null ? voucher.getBranch().getBranchId() : null;
            if (!Objects.equals(voucherBranchId, bookingBranchId)) {
                return invalid(code, "voucher.apply.branch_mismatch");
            }
        }

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
