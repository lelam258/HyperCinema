package com.cinema.hyperCinema.dto.ui.booking;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PosSummaryView {

    private ShowtimeOptionView showtime;

    private List<SeatAvailabilityView> selectedSeats;

    private List<FoodAddonOptionView> selectedFoodItems;

    private VoucherPreviewView voucher;

    private long subtotal;

    private long discount;

    private long total;

    private String displaySubtotal;

    private String displayDiscount;

    private String displayTotal;

    private List<String> validationMessages;
}
