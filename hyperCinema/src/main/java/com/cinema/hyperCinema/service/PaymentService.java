package com.cinema.hyperCinema.service;

import com.cinema.hyperCinema.model.Booking;
import com.cinema.hyperCinema.model.Payment;
import com.cinema.hyperCinema.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Map;

public interface PaymentService {

    Page<Payment> getPaymentHistory(User user, String status, String method, LocalDate startDate, LocalDate endDate, Pageable pageable);

    Payment getPaymentById(Integer paymentId, User user);

    String createVNPayUrl(Booking booking, String ipAddress);

    boolean verifyAndProcessCallback(Map<String, String> fields);
}
