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
public class VoucherPreviewView {

    private String code;

    private boolean valid;

    private long discountAmount;

    private String displayDiscount;

    private String message;
}
