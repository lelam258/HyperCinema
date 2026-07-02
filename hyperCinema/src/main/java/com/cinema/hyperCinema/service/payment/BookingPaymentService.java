package com.cinema.hyperCinema.service.payment;

import java.time.LocalDateTime;
import java.util.Optional;

import com.cinema.hyperCinema.model.Payment;

public interface BookingPaymentService {

    Optional<Payment> findPaymentByBookingId(Integer bookingId);

    void confirmPayment(Integer bookingId);

    void failPayment(Integer bookingId);

    int expirePendingPayments(LocalDateTime now);
}
