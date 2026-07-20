package com.cinema.hyperCinema.service.impl;

import com.cinema.hyperCinema.config.VNPayProperties;
import com.cinema.hyperCinema.model.Booking;
import com.cinema.hyperCinema.model.Payment;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.repository.PaymentRepository;
import com.cinema.hyperCinema.repository.PaymentSpecifications;
import com.cinema.hyperCinema.service.PaymentService;
import com.cinema.hyperCinema.service.payment.BookingPaymentService;
import com.cinema.hyperCinema.service.payment.VNPayService;
import com.cinema.hyperCinema.util.VNPayUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final VNPayProperties vnpayProperties;
    private final VNPayService vnPayService;
    private final BookingPaymentService bookingPaymentService;

    @Override
    public Page<Payment> getPaymentHistory(User user, String status, String method, LocalDate startDate,
                                           LocalDate endDate, Pageable pageable) {
        Integer branchId = null;
        Integer userId = null;

        String roleName = user.getRole().getName();
        if ("Customer".equalsIgnoreCase(roleName)) {
            userId = user.getUserId();
        } else if ("Staff".equalsIgnoreCase(roleName)
                || "BranchManager".equalsIgnoreCase(roleName)
                || "Branch_Manager".equalsIgnoreCase(roleName)) {
            branchId = user.getBranch() != null ? user.getBranch().getBranchId() : -1;
        }

        Specification<Payment> spec = PaymentSpecifications.filter(status, method, startDate, endDate, branchId, userId);
        return paymentRepository.findAll(spec, pageable);
    }

    @Override
    public Payment getPaymentById(Integer paymentId, User user) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay giao dich ID: " + paymentId));

        String roleName = user.getRole().getName();
        if ("Customer".equalsIgnoreCase(roleName)) {
            if (!payment.getBooking().getUser().getUserId().equals(user.getUserId())) {
                throw new SecurityException("Ban khong co quyen xem chi tiet giao dich nay.");
            }
        } else if ("Staff".equalsIgnoreCase(roleName)
                || "BranchManager".equalsIgnoreCase(roleName)
                || "Branch_Manager".equalsIgnoreCase(roleName)) {
            Integer userBranchId = user.getBranch() != null ? user.getBranch().getBranchId() : null;
            Integer paymentBranchId = payment.getBooking().getShowtime().getHall().getBranch().getBranchId();
            if (userBranchId == null || !userBranchId.equals(paymentBranchId)) {
                throw new SecurityException("Ban khong co quyen xem giao dich cua chi nhanh khac.");
            }
        }
        return payment;
    }

    @Override
    public String createVNPayUrl(Booking booking, String ipAddress) {
        if (!vnpayProperties.isConfigured()) {
            throw new IllegalStateException("VNPay is not configured");
        }

        String vnpTxnRef = String.valueOf(booking.getBookingId());
        String vnpOrderInfo = "Thanh toan dat ve xem phim booking ID: " + booking.getBookingId();
        long amount = booking.getTotalPrice() * 100;

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", vnpayProperties.version());
        vnpParams.put("vnp_Command", vnpayProperties.command());
        vnpParams.put("vnp_TmnCode", vnpayProperties.tmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(amount));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", vnpTxnRef);
        vnpParams.put("vnp_OrderInfo", vnpOrderInfo);
        vnpParams.put("vnp_OrderType", vnpayProperties.orderType());
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnpayProperties.returnUrl());
        vnpParams.put("vnp_IpAddr", ipAddress);
        vnpParams.put("vnp_CreateDate", LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));

        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        for (String fieldName : fieldNames) {
            String fieldValue = vnpParams.get(fieldName);
            if (fieldValue == null || fieldValue.isEmpty()) {
                continue;
            }
            hashData.append(fieldName).append('=').append(encode(fieldValue));
            query.append(encode(fieldName)).append('=').append(encode(fieldValue));
            hashData.append('&');
            query.append('&');
        }
        if (hashData.length() > 0) {
            hashData.deleteCharAt(hashData.length() - 1);
            query.deleteCharAt(query.length() - 1);
        }

        String secureHash = VNPayUtil.hmacSHA512(vnpayProperties.hashSecret(), hashData.toString());
        return vnpayProperties.payUrl() + "?" + query + "&vnp_SecureHash=" + secureHash;
    }

    @Override
    @Transactional
    public boolean verifyAndProcessCallback(Map<String, String> fields) {
        if (!vnPayService.isValidCallback(fields)) {
            return false;
        }
        Integer bookingId = vnPayService.bookingId(fields).orElse(null);
        Long callbackAmount = vnPayService.callbackAmount(fields).orElse(null);
        if (bookingId == null || callbackAmount == null) {
            return false;
        }
        return vnPayService.isSuccessful(fields)
                ? bookingPaymentService.confirmOnlinePayment(bookingId, callbackAmount).accepted()
                : bookingPaymentService.failOnlinePayment(bookingId, callbackAmount).accepted();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
