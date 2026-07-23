package com.cinema.hyperCinema.controller.manager;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
import com.cinema.hyperCinema.exception.booking.BookingManagementException;
import com.cinema.hyperCinema.security.CustomUserDetails;
import com.cinema.hyperCinema.service.booking.BookingManagementService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/manager/bookings")
@RequiredArgsConstructor
public class ManagerBookingManagementController {

    private static final int DEFAULT_PAGE_SIZE = 12;

    private final BookingManagementService bookingManagementService;

    @GetMapping
    public String list(@AuthenticationPrincipal CustomUserDetails principal,
                       @ModelAttribute("filter") BookingManagementFilter filter,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "12") int size,
                       Model model) {
        var pageable = PageRequest.of(Math.max(page, 0), safeSize(size),
                Sort.by(Sort.Direction.DESC, "bookingId")
                        .and(Sort.by(Sort.Direction.DESC, "createdAt")));
        var bookings = bookingManagementService.findBookings(principal.getUser(), filter, pageable);
        model.addAttribute("bookings", bookings);
        model.addAttribute("summary", bookingManagementService.summarize(principal.getUser(), filter));
        addManagerContext(principal, model);
        return "manager/bookings/list";
    }

    @GetMapping("/{bookingId}")
    public String detail(@PathVariable Integer bookingId,
                         @AuthenticationPrincipal CustomUserDetails principal,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        try {
            model.addAttribute("booking", bookingManagementService.findDetail(principal.getUser(), bookingId));
            addManagerContext(principal, model);
            return "manager/bookings/detail";
        } catch (BookingManagementException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessageKey());
            return "redirect:/manager/bookings";
        }
    }

    @PostMapping("/{bookingId}/confirm-payment")
    public String confirmPayment(@PathVariable Integer bookingId,
                                 @AuthenticationPrincipal CustomUserDetails principal,
                                 RedirectAttributes redirectAttributes) {
        return performAction(bookingId, redirectAttributes,
                () -> bookingManagementService.confirmPayment(principal.getUser(), bookingId),
                "booking.management.payment_confirmed");
    }

    @PostMapping("/{bookingId}/mark-served")
    public String markServed(@PathVariable Integer bookingId,
                             @AuthenticationPrincipal CustomUserDetails principal,
                             RedirectAttributes redirectAttributes) {
        return performAction(bookingId, redirectAttributes,
                () -> bookingManagementService.markServed(principal.getUser(), bookingId),
                "booking.management.served");
    }

    @PostMapping("/{bookingId}/cancel")
    public String cancel(@PathVariable Integer bookingId,
                         @AuthenticationPrincipal CustomUserDetails principal,
                         RedirectAttributes redirectAttributes) {
        return performAction(bookingId, redirectAttributes,
                () -> bookingManagementService.cancel(principal.getUser(), bookingId),
                "booking.management.cancelled");
    }

    private String performAction(Integer bookingId,
                                 RedirectAttributes redirectAttributes,
                                 Runnable action,
                                 String successKey) {
        try {
            action.run();
            redirectAttributes.addFlashAttribute("successMessage", successKey);
        } catch (BookingManagementException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessageKey());
        }
        return "redirect:/manager/bookings/" + bookingId;
    }

    private void addManagerContext(CustomUserDetails principal, Model model) {
        var user = principal.getUser();
        model.addAttribute("managerName", user.getFullName());
        model.addAttribute("branchName", user.getBranch() != null ? user.getBranch().getName() : "Chua phan cong chi nhanh");
    }

    private int safeSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, 50);
    }
}
