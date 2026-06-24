package com.cinema.hyperCinema.dto.admin.voucher.request;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VoucherCreateRequest {

    @NotBlank
    @Size(max = 255, message = "{voucher.title.invalid}")
    private String title;

    @NotBlank
    @Size(max = 50, message = "{voucher.code.invalid}")
    private String code;

    @NotBlank(message = "{voucher.discount_type.invalid}")
    private String discountType;          // DiscountType: PERCENTAGE | FIXED_AMOUNT

    @NotNull
    @Min(value = 1, message = "{voucher.discount_value.invalid}")
    private Integer discountValue;

    @NotNull
    @Min(value = 0, message = "{voucher.min_order_value.invalid}")
    private Integer minOrderValue;

    @NotNull
    @Min(value = 1, message = "{voucher.max_uses.invalid}")
    private Integer maxUses;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDate;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endDate;

    private boolean branchSpecific;

    private Integer branchId;             // bắt buộc khi branchSpecific = true
}
