package com.cinema.hyperCinema.dto.ui.booking;

import java.util.List;

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
