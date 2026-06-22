package com.cinema.hyperCinema.dto.admin.voucher.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherDetailView {

    private Integer voucherId;

    private String title;

    private String code;

    private String discountType;

    private Integer discountValue;

    private Integer minOrderValue;

    private Integer maxUses;

    private Integer usedCount;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private boolean branchSpecific;

    private Integer branchId;

    private String branchName;

    private String status;
}
