package com.cinema.hyperCinema.dto.admin.voucher.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherPreview {

    private boolean valid;

    private String code;

    private long discountAmount;

    private long finalPrice;

    private String errorKey;       // null khi valid = true
}
