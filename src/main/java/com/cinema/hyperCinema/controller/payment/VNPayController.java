package com.cinema.hyperCinema.controller.payment;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cinema.hyperCinema.model.Booking;
import com.cinema.hyperCinema.service.booking.BookingService;
import com.cinema.hyperCinema.service.payment.BookingPaymentService;
import com.cinema.hyperCinema.service.payment.VNPayService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class VNPayController {

    private final VNPayService vnPayService;
    private final BookingService bookingService;
    private final BookingPaymentService bookingPaymentService;

    public VNPayController(VNPayService vnPayService,
                           BookingService bookingService,
                           BookingPaymentService bookingPaymentService) {
        this.vnPayService = vnPayService;
        this.bookingService = bookingService;
        this.bookingPaymentService = bookingPaymentService;
    }

    @GetMapping({"/vnpay-return", "/api/payment/vnpay-return"})
    public String handleReturn(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        Map<String, String> params = flattenParams(request);
        Integer bookingId = parseBookingId(params.get("vnp_TxnRef"));
        if (bookingId == null) {
            redirectAttributes.addFlashAttribute("bookingError", "Khong tim thay ma booking tu VNPay.");
            return "redirect:/my/dashboard";
        }

        Booking booking = bookingService.findById(bookingId).orElse(null);
        if (booking == null) {
            redirectAttributes.addFlashAttribute("bookingError", "Booking khong ton tai.");
            return "redirect:/my/dashboard";
        }

        if (!vnPayService.isValidReturn(params)) {
            bookingPaymentService.failPayment(bookingId);
            redirectAttributes.addFlashAttribute("bookingError", "Xac thuc thanh toan VNPay that bai.");
            return "redirect:/booking?showtimeId=" + booking.getShowtime().getShowtimeId();
        }

        if (vnPayService.isSuccessful(params)) {
            bookingPaymentService.confirmPayment(bookingId);
            redirectAttributes.addFlashAttribute("bookingSuccess", "Thanh toan VNPay thanh cong.");
            return "redirect:/my/bookings";
        }

        bookingPaymentService.failPayment(bookingId);
        redirectAttributes.addFlashAttribute("bookingError", "Thanh toan VNPay chua hoan tat. Vui long chon lai ghe.");
        return "redirect:/booking?showtimeId=" + booking.getShowtime().getShowtimeId();
    }

    private Integer parseBookingId(String value) {
        try {
            return value == null ? null : Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Map<String, String> flattenParams(HttpServletRequest request) {
        return request.getParameterMap().entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().length > 0)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> Arrays.stream(entry.getValue()).findFirst().orElse("")));
    }
}
