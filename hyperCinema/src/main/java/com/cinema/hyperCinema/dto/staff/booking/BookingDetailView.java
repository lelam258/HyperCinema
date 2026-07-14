package com.cinema.hyperCinema.dto.staff.booking;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookingDetailView {

    private final Integer bookingId;
    private final String customerName;
    private final String customerEmail;
    private final String customerPhone;
    private final String movieTitle;
    private final String branchName;
    private final String hallName;
    private final LocalDateTime showtimeStart;
    private final LocalDateTime showtimeEnd;
    private final LocalDateTime createdAt;
    private final Long seatSubtotal;
    private final Long foodSubtotal;
    private final Long orderSubtotal;
    private final Long voucherDiscountAmount;
    private final Long membershipDiscountBase;
    private final Long membershipDiscountAmount;
    private final String membershipPlanName;
    private final BigDecimal membershipDiscountPercent;
    private final Long totalPrice;
    private final String bookingStatus;
    private final String paymentStatus;
    private final String paymentMethod;
    private final Long paymentAmount;
    private final String promotionName;
    private final List<BookingTicketView> tickets;
    private final List<BookingFoodOrderView> foodOrders;
    private final boolean canConfirmPayment;
    private final boolean canMarkServed;
    private final boolean canCancel;
}
