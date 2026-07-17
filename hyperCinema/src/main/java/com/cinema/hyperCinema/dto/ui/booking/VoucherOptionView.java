package com.cinema.hyperCinema.dto.ui.booking;

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
public class VoucherOptionView {

    private Integer promotionId;

    private String code;

    private String title;

    private String discountLabel;

    private Integer minOrderValue;

    private String displayMinOrderValue;

    private String validUntil;

    private String branchScope;

    private boolean eligible;

    private String disabledReason;
}
