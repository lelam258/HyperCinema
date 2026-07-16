package com.cinema.hyperCinema.service.booking;

import java.math.BigDecimal;

import com.cinema.hyperCinema.model.Promotion;
import com.cinema.hyperCinema.model.UserMembership;

public record BookingPricingResult(
        long seatSubtotal,
        long foodSubtotal,
        long orderSubtotal,
        Promotion promotion,
        String voucherCode,
        long voucherDiscountAmount,
        UserMembership membership,
        String membershipPlanName,
        BigDecimal membershipDiscountPercent,
        long membershipDiscountBase,
        long membershipDiscountAmount,
        long finalTotal) {
}
