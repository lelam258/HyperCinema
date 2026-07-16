package com.cinema.hyperCinema.service.impl;

import com.cinema.hyperCinema.config.VNPayProperties;
import com.cinema.hyperCinema.model.Booking;
import com.cinema.hyperCinema.model.Payment;
import com.cinema.hyperCinema.model.Ticket;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.repository.BookingRepository;
import com.cinema.hyperCinema.repository.PaymentRepository;
import com.cinema.hyperCinema.repository.PaymentSpecifications;
import com.cinema.hyperCinema.repository.TicketRepository;
import com.cinema.hyperCinema.service.PaymentService;
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
import java.util.*;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final VNPayProperties vnpayProperties;

    @Override
    public Page<Payment> getPaymentHistory(User user, String status, String method, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Integer branchId = null;
        Integer userId = null;

        String roleName = user.getRole().getName();
        if ("Customer".equalsIgnoreCase(roleName)) {
            userId = user.getUserId();
        } else if ("Staff".equalsIgnoreCase(roleName) || "BranchManager".equalsIgnoreCase(roleName) || "Branch_Manager".equalsIgnoreCase(roleName)) {
            if (user.getBranch() != null) {
                branchId = user.getBranch().getBranchId();
            } else {
                branchId = -1; // Unassigned staff see no branch payments
            }
        }

        Specification<Payment> spec = PaymentSpecifications.filter(status, method, startDate, endDate, branchId, userId);
        return paymentRepository.findAll(spec, pageable);
    }

    @Override
    public Payment getPaymentById(Integer paymentId, User user) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch ID: " + paymentId));

        String roleName = user.getRole().getName();
        if ("Customer".equalsIgnoreCase(roleName)) {
            if (!payment.getBooking().getUser().getUserId().equals(user.getUserId())) {
                throw new SecurityException("Bạn không có quyền xem chi tiết giao dịch này.");
            }
        } else if ("Staff".equalsIgnoreCase(roleName) || "BranchManager".equalsIgnoreCase(roleName) || "Branch_Manager".equalsIgnoreCase(roleName)) {
            Integer userBranchId = user.getBranch() != null ? user.getBranch().getBranchId() : null;
            Integer paymentBranchId = payment.getBooking().getShowtime().getHall().getBranch().getBranchId();
            if (userBranchId == null || !userBranchId.equals(paymentBranchId)) {
                throw new SecurityException("Bạn không có quyền xem giao dịch của chi nhánh khác.");
            }
        }
        return payment;
    }

    @Override
    public String createVNPayUrl(Booking booking, String ipAddress) {
        String vnp_Version = vnpayProperties.version();
        String vnp_Command = vnpayProperties.command();
        String vnp_TmnCode = vnpayProperties.tmnCode();
        String vnp_CurrCode = "VND";
        String vnp_TxnRef = String.valueOf(booking.getBookingId());
        String vnp_OrderInfo = "Thanh toan dat ve xem phim booking ID: " + booking.getBookingId();
        String vnp_OrderType = vnpayProperties.orderType();
        String vnp_Locale = "vn";
        String vnp_ReturnUrl = vnpayProperties.returnUrl();
        
        long amount = booking.getTotalPrice() * 100; // VNPay expects amount in cents
        
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", vnp_CurrCode);
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
        vnp_Params.put("vnp_OrderType", vnp_OrderType);
        vnp_Params.put("vnp_Locale", vnp_Locale);
        vnp_Params.put("vnp_ReturnUrl", vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", ipAddress);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String vnp_CreateDate = LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).format(formatter);
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        
        try {
            for (String fieldName : fieldNames) {
                String fieldValue = vnp_Params.get(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8).replace("+", "%20"));
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8)).append('=')
                         .append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8).replace("+", "%20"));
                         
                    hashData.append('&');
                    query.append('&');
                }
            }
            if (hashData.length() > 0) {
                hashData.deleteCharAt(hashData.length() - 1);
                query.deleteCharAt(query.length() - 1);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error encoding VNPay parameters", e);
        }

        String secureHash = VNPayUtil.hmacSHA512(vnpayProperties.hashSecret(), hashData.toString());
        return vnpayProperties.payUrl() + "?" + query.toString() + "&vnp_SecureHash=" + secureHash;
    }

    @Override
    @Transactional
    public boolean verifyAndProcessCallback(Map<String, String> fields) {
        String secureHash = fields.get("vnp_SecureHash");
        if (secureHash == null || secureHash.isEmpty()) {
            return false;
        }

        Map<String, String> vnp_Params = new HashMap<>();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key != null && key.startsWith("vnp_") && !key.equals("vnp_SecureHash") && !key.equals("vnp_SecureHashType")) {
                vnp_Params.put(key, value);
            }
        }

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        try {
            for (String fieldName : fieldNames) {
                String fieldValue = vnp_Params.get(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8).replace("+", "%20"));
                    hashData.append('&');
                }
            }
            if (hashData.length() > 0) {
                hashData.deleteCharAt(hashData.length() - 1);
            }
        } catch (Exception e) {
            return false;
        }

        String checkSum = VNPayUtil.hmacSHA512(vnpayProperties.hashSecret(), hashData.toString());
        if (!checkSum.equalsIgnoreCase(secureHash)) {
            return false; // Signatures mismatch
        }

        String responseCode = fields.get("vnp_ResponseCode");
        String txnRef = fields.get("vnp_TxnRef");
        Integer bookingId = Integer.parseInt(txnRef);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Booking ID: " + bookingId));

        if ("Confirmed".equals(booking.getStatus()) || "Completed".equals(booking.getStatus())) {
            return true; // Already processed successfully
        }
        if ("Cancelled".equals(booking.getStatus())) {
            return false; // Already cancelled
        }

        Payment payment = booking.getPayment();
        if (payment == null) {
            payment = new Payment();
            payment.setBooking(booking);
            payment.setAmount(booking.getTotalPrice());
            payment.setMethod("VNPay");
        }

        if ("00".equals(responseCode)) {
            booking.setStatus("Confirmed");
            payment.setStatus("Completed");
            
            if (booking.getTickets() != null) {
                for (Ticket ticket : booking.getTickets()) {
                    ticket.setStatus("Active");
                    ticketRepository.save(ticket);
                }
            }
        } else {
            booking.setStatus("Cancelled");
            payment.setStatus("Failed");
            
            if (booking.getTickets() != null) {
                for (Ticket ticket : booking.getTickets()) {
                    ticket.setStatus("Cancelled");
                    ticketRepository.save(ticket);
                }
            }
        }

        bookingRepository.save(booking);
        paymentRepository.save(payment);
        return "00".equals(responseCode);
    }
}
