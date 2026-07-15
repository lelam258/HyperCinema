package com.cinema.hyperCinema.controller.payment;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cinema.hyperCinema.model.Booking;
import com.cinema.hyperCinema.security.CustomUserDetails;
import com.cinema.hyperCinema.service.booking.BookingService;
import com.cinema.hyperCinema.service.payment.BookingPaymentService;

@Controller
public class VietQrPaymentController {

    private final BookingService bookingService;
    private final BookingPaymentService bookingPaymentService;
    private final String bankId;
    private final String accountNo;
    private final String accountName;
    private final String qrTemplate;

    public VietQrPaymentController(BookingService bookingService,
                                   BookingPaymentService bookingPaymentService,
                                   @Value("${vietqr.bank-id:}") String bankId,
                                   @Value("${vietqr.account-no:}") String accountNo,
                                   @Value("${vietqr.account-name:}") String accountName,
                                   @Value("${vietqr.template:compact2}") String qrTemplate) {
        this.bookingService = bookingService;
        this.bookingPaymentService = bookingPaymentService;
        this.bankId = bankId.trim();
        this.accountNo = accountNo.trim();
        this.accountName = accountName.trim();
        this.qrTemplate = qrTemplate.trim().isEmpty() ? "compact2" : qrTemplate.trim();
    }

    @GetMapping("/payment/vietqr/{bookingId}")
    public String paymentPage(@PathVariable Integer bookingId,
                              @AuthenticationPrincipal CustomUserDetails principal,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        Booking booking = bookingService.findById(bookingId).orElse(null);
        if (booking == null || !canAccess(booking, principal)) {
            redirectAttributes.addFlashAttribute("bookingError", "Khong tim thay booking can thanh toan.");
            return "redirect:/my/dashboard";
        }

        String transferContent = transferContent(bookingId);
        model.addAttribute("booking", booking);
        model.addAttribute("payment", bookingPaymentService.findPaymentByBookingId(bookingId).orElse(null));
        model.addAttribute("transferContent", transferContent);
        model.addAttribute("bankId", bankId);
        model.addAttribute("accountNo", accountNo);
        model.addAttribute("accountName", accountName);
        model.addAttribute("qrUrl", qrUrl(booking, transferContent));
        model.addAttribute("vietQrConfigured", isConfigured());
        return "my/vietqr-payment";
    }

    @PostMapping("/payment/vietqr/{bookingId}/confirm")
    public String confirmPayment(@PathVariable Integer bookingId,
                                 @AuthenticationPrincipal CustomUserDetails principal,
                                 RedirectAttributes redirectAttributes) {
        Booking booking = bookingService.findById(bookingId).orElse(null);
        if (booking == null || !canAccess(booking, principal)) {
            redirectAttributes.addFlashAttribute("bookingError", "Khong tim thay booking can xac nhan.");
            return "redirect:/my/dashboard";
        }

        try {
            bookingPaymentService.confirmPayment(bookingId);
            redirectAttributes.addFlashAttribute("bookingSuccess", "Da xac nhan thanh toan VietQR.");
            return "redirect:/my/bookings";
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("bookingError", ex.getMessage());
            return "redirect:/booking?showtimeId=" + booking.getShowtime().getShowtimeId();
        }
    }

    private boolean canAccess(Booking booking, CustomUserDetails principal) {
        if (principal == null || principal.getUser() == null) {
            return false;
        }
        return booking.getUser() != null
                && booking.getUser().getUserId().equals(principal.getUser().getUserId());
    }

    private boolean isConfigured() {
        return !bankId.isBlank() && !accountNo.isBlank();
    }

    private String transferContent(Integer bookingId) {
        return "HC" + bookingId;
    }

    private String qrUrl(Booking booking, String transferContent) {
        if (!isConfigured()) {
            return "";
        }
        return "https://img.vietqr.io/image/"
                + encode(bankId) + "-" + encode(accountNo) + "-" + encode(qrTemplate) + ".png"
                + "?amount=" + booking.getTotalPrice()
                + "&addInfo=" + encode(transferContent)
                + "&accountName=" + encode(accountName);
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
