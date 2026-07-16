package com.cinema.hyperCinema.scheduler;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.cinema.hyperCinema.service.payment.BookingPaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingPaymentExpiryScheduler {

    private final BookingPaymentService bookingPaymentService;

    @Scheduled(fixedRateString = "${booking.payment.expiry-scheduler-rate-ms:30000}")
    public void expirePendingPayments() {
        try {
            int expiredCount = bookingPaymentService.expirePendingPayments(LocalDateTime.now());
            if (expiredCount > 0) {
                log.info("Expired {} pending booking payment(s).", expiredCount);
            }
        } catch (RuntimeException ex) {
            log.error("Failed to expire pending booking payments.", ex);
        }
    }
}
