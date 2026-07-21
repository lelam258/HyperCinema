package com.cinema.hyperCinema.controller.payment;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cinema.hyperCinema.model.Booking;
import com.cinema.hyperCinema.service.booking.BookingService;
import com.cinema.hyperCinema.service.payment.BookingPaymentService;
import com.cinema.hyperCinema.service.payment.PaymentCallbackResult;
import com.cinema.hyperCinema.service.payment.VNPayService;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.cinema.hyperCinema.security.CustomUserDetails;

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
    public String handleReturn(HttpServletRequest request,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        Map<String, String> params = flattenParams(request);
        Integer bookingId = vnPayService.bookingId(params).orElse(null);
        Long callbackAmount = vnPayService.callbackAmount(params).orElse(null);

        boolean isCustomer = userDetails == null || userDetails.getAuthorities().stream()
                .anyMatch(auth -> "ROLE_CUSTOMER".equals(auth.getAuthority()));

        if (bookingId == null || callbackAmount == null) {
            redirectAttributes.addFlashAttribute("bookingError", "Không tìm thấy mã booking từ VNPay.");
            return isCustomer ? "redirect:/my/dashboard" : "redirect:/staff/booking";
        }

        Booking booking = bookingService.findById(bookingId).orElse(null);
        if (booking == null) {
            redirectAttributes.addFlashAttribute("bookingError", "Booking không tồn tại.");
            return isCustomer ? "redirect:/my/dashboard" : "redirect:/staff/booking";
        }

        String errorRedirect = isCustomer
                ? "redirect:/booking?showtimeId=" + booking.getShowtime().getShowtimeId()
                : "redirect:/staff/booking";

        if (!vnPayService.isValidCallback(params)) {
            redirectAttributes.addFlashAttribute("bookingError", "Xác thực thanh toán VNPay thất bại.");
            return errorRedirect;
        }

        if (vnPayService.isSuccessful(params)) {
            PaymentCallbackResult result = bookingPaymentService.confirmOnlinePayment(bookingId, callbackAmount);
            if (result.accepted()) {
                redirectAttributes.addFlashAttribute("bookingSuccess", "Thanh toán VNPay thành công.");
                return isCustomer ? "redirect:/my/bookings" : "redirect:/staff/bookings";
            }
            redirectAttributes.addFlashAttribute("bookingError", result.message());
            return errorRedirect;
        }

        PaymentCallbackResult result = bookingPaymentService.failOnlinePayment(bookingId, callbackAmount);
        if (result.status() == PaymentCallbackResult.Status.INVALID_AMOUNT) {
            redirectAttributes.addFlashAttribute("bookingError", result.message());
            return errorRedirect;
        }
        redirectAttributes.addFlashAttribute("bookingError", "Thanh toán VNPay chưa hoàn tất. Vui lòng chọn lại ghế.");
        return errorRedirect;
    }

    @GetMapping("/api/payment/vnpay-ipn")
    @ResponseBody
    public Map<String, String> handleIpn(HttpServletRequest request) {
        try {
            Map<String, String> params = flattenParams(request);
            if (!vnPayService.isValidCallback(params)) {
                return ipnResponse("97", "Invalid Checksum");
            }

            Integer bookingId = vnPayService.bookingId(params).orElse(null);
            if (bookingId == null) {
                return ipnResponse("01", "Order not Found");
            }

            Long callbackAmount = vnPayService.callbackAmount(params).orElse(null);
            if (callbackAmount == null) {
                return ipnResponse("04", "Invalid Amount");
            }

            PaymentCallbackResult result = vnPayService.isSuccessful(params)
                    ? bookingPaymentService.confirmOnlinePayment(bookingId, callbackAmount)
                    : bookingPaymentService.failOnlinePayment(bookingId, callbackAmount);
            return switch (result.status()) {
                case CONFIRMED -> ipnResponse("00", "Confirm Success");
                case ALREADY_CONFIRMED -> ipnResponse("02", "Order already confirmed");
                case ORDER_NOT_FOUND, PAYMENT_NOT_FOUND -> ipnResponse("01", "Order not Found");
                case INVALID_AMOUNT -> ipnResponse("04", "Invalid Amount");
                case EXPIRED, INVALID_STATE -> ipnResponse("02", result.message());
                case FAILED -> ipnResponse("00", "Confirm Success");
            };
        } catch (RuntimeException ex) {
            return ipnResponse("99", "Unknown error");
        }
    }

    private Map<String, String> ipnResponse(String code, String message) {
        return Map.of("RspCode", code, "Message", message);
    }

    private Map<String, String> flattenParams(HttpServletRequest request) {
        return request.getParameterMap().entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().length > 0)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> Arrays.stream(entry.getValue()).findFirst().orElse("")));
    }
}
