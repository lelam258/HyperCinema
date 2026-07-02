package com.cinema.hyperCinema.dto.admin.voucher.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherListItem {

    private Integer voucherId;

    private String code;

    private String title;

    private String discountType;

    private Integer discountValue;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private Integer usedCount;

    private Integer maxUses;

    private String status;

    private boolean expired;       // chỉ báo trực quan (Requirement 6.3)
}
