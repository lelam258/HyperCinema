package com.cinema.hyperCinema.controller.staff;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cinema.hyperCinema.dto.staff.booking.BookingManagementFilter;
import com.cinema.hyperCinema.dto.staff.booking.BookingDetailView;
import com.cinema.hyperCinema.exception.booking.BookingManagementException;
import com.cinema.hyperCinema.security.CustomUserDetails;
import com.cinema.hyperCinema.service.booking.BookingManagementService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/staff/bookings")
@RequiredArgsConstructor
public class StaffBookingManagementController {

    private static final int DEFAULT_PAGE_SIZE = 12;

    private final BookingManagementService bookingManagementService;

    @Value("${vietqr.bank-id:}")
    private String bankId;

    @Value("${vietqr.account-no:}")
    private String accountNo;

    @Value("${vietqr.account-name:}")
    private String accountName;

    @Value("${vietqr.template:compact2}")
    private String qrTemplate;

    @GetMapping
    public String list(@AuthenticationPrincipal CustomUserDetails principal,
                       @ModelAttribute("filter") BookingManagementFilter filter,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "12") int size,
                       Model model) {
        var pageable = PageRequest.of(Math.max(page, 0), safeSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        var bookings = bookingManagementService.findBookings(principal.getUser(), filter, pageable);
        model.addAttribute("bookings", bookings);
        model.addAttribute("summary", bookingManagementService.summarize(bookings));
        addStaffContext(principal, model);
        return "staff/bookings/list";
    }

    @GetMapping("/{bookingId}")
    public String detail(@PathVariable Integer bookingId,
                         @AuthenticationPrincipal CustomUserDetails principal,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        try {
            BookingDetailView booking = bookingManagementService.findDetail(principal.getUser(), bookingId);
            model.addAttribute("booking", booking);
            addVietQrContext(booking, model);
            addStaffContext(principal, model);
            return "staff/bookings/detail";
        } catch (BookingManagementException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessageKey());
            return "redirect:/staff/bookings";
        }
    }

    @PostMapping("/{bookingId}/confirm-payment")
    public String confirmPayment(@PathVariable Integer bookingId,
                                 @AuthenticationPrincipal CustomUserDetails principal,
                                 RedirectAttributes redirectAttributes) {
        return performAction(bookingId, principal, redirectAttributes,
                () -> bookingManagementService.confirmPayment(principal.getUser(), bookingId),
                "booking.management.payment_confirmed");
    }

    @PostMapping("/{bookingId}/mark-served")
    public String markServed(@PathVariable Integer bookingId,
                             @AuthenticationPrincipal CustomUserDetails principal,
                             RedirectAttributes redirectAttributes) {
        return performAction(bookingId, principal, redirectAttributes,
                () -> bookingManagementService.markServed(principal.getUser(), bookingId),
                "booking.management.served");
    }

    @PostMapping("/{bookingId}/cancel")
    public String cancel(@PathVariable Integer bookingId,
                         @AuthenticationPrincipal CustomUserDetails principal,
                         RedirectAttributes redirectAttributes) {
        return performAction(bookingId, principal, redirectAttributes,
                () -> bookingManagementService.cancel(principal.getUser(), bookingId),
                "booking.management.cancelled");
    }

    private String performAction(Integer bookingId,
                                 CustomUserDetails principal,
                                 RedirectAttributes redirectAttributes,
                                 Runnable action,
                                 String successKey) {
        try {
            action.run();
            redirectAttributes.addFlashAttribute("successMessage", successKey);
        } catch (BookingManagementException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessageKey());
        }
        return "redirect:/staff/bookings/" + bookingId;
    }

    private void addStaffContext(CustomUserDetails principal, Model model) {
        var user = principal.getUser();
        model.addAttribute("staffName", user.getFullName());
        model.addAttribute("branchName", user.getBranch() != null ? user.getBranch().getName() : "Chua phan cong chi nhanh");
    }

    private void addVietQrContext(BookingDetailView booking, Model model) {
        boolean vietQrPayment = "VietQR".equalsIgnoreCase(blankToEmpty(booking.getPaymentMethod()));
        String transferContent = transferContent(booking.getBookingId());
        model.addAttribute("staffVietQrPayment", vietQrPayment);
        model.addAttribute("vietQrConfigured", isVietQrConfigured());
        model.addAttribute("transferContent", transferContent);
        model.addAttribute("bankId", clean(bankId));
        model.addAttribute("accountNo", clean(accountNo));
        model.addAttribute("accountName", clean(accountName));
        model.addAttribute("qrUrl", vietQrPayment ? qrUrl(booking, transferContent) : "");
    }

    private boolean isVietQrConfigured() {
        return !clean(bankId).isBlank() && !clean(accountNo).isBlank();
    }

    private String transferContent(Integer bookingId) {
        return "HC" + bookingId;
    }

    private String qrUrl(BookingDetailView booking, String transferContent) {
        if (!isVietQrConfigured()) {
            return "";
        }
        return "https://img.vietqr.io/image/"
                + encode(clean(bankId)) + "-" + encode(clean(accountNo)) + "-" + encode(qrTemplate()) + ".png"
                + "?amount=" + (booking.getTotalPrice() != null ? booking.getTotalPrice() : 0)
                + "&addInfo=" + encode(transferContent)
                + "&accountName=" + encode(clean(accountName));
    }

    private String qrTemplate() {
        String value = clean(qrTemplate);
        return value.isBlank() ? "compact2" : value;
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private int safeSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, 50);
    }
}
