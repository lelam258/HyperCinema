package com.cinema.hyperCinema.controller.customer;

import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.model.Booking;
import com.cinema.hyperCinema.model.Payment;
import com.cinema.hyperCinema.repository.BookingRepository;
import com.cinema.hyperCinema.security.CustomUserDetails;
import com.cinema.hyperCinema.service.PaymentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/my")
public class CustomerDashboardController {

    private final BookingRepository bookingRepository;
    private final PaymentService paymentService;

    public CustomerDashboardController(BookingRepository bookingRepository, PaymentService paymentService) {
        this.bookingRepository = bookingRepository;
        this.paymentService = paymentService;
    }

    @GetMapping({"/dashboard", ""})
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User user = userDetails.getUser();
        
        model.addAttribute("customerName", user.getFullName());
        model.addAttribute("email", user.getEmail());
        model.addAttribute("phone", user.getPhone());
        
        // Mock data cho Customer (điểm thưởng, hạng thành viên)
        model.addAttribute("rewardPoints", 1250);
        model.addAttribute("membershipTier", "Vàng");

        // ── Meta ──
        model.addAttribute("lastUpdated",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));

        return "my/dashboard";
    }

    @GetMapping("/bookings")
    public String bookingHistory(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User user = userDetails.getUser();
        List<Booking> bookings = bookingRepository.findByUser_UserIdOrderByCreatedAtDesc(user.getUserId());
        
        model.addAttribute("customerName", user.getFullName());
        model.addAttribute("bookings", bookings);
        
        model.addAttribute("lastUpdated",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));
                
        return "my/bookings";
    }

    @GetMapping("/payments")
    public String paymentHistory(
            @RequestParam(name = "status", required = false, defaultValue = "All") String status,
            @RequestParam(name = "method", required = false, defaultValue = "All") String method,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {
        User user = userDetails.getUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Payment> paymentPage = paymentService.getPaymentHistory(user, status, method, null, null, pageable);

        model.addAttribute("customerName", user.getFullName());
        model.addAttribute("payments", paymentPage);
        model.addAttribute("status", status);
        model.addAttribute("method", method);
        model.addAttribute("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));

        return "my/payment/list";
    }

    @GetMapping("/payments/{paymentId}")
    public String paymentDetail(
            @PathVariable Integer paymentId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {
        User user = userDetails.getUser();
        Payment payment = paymentService.getPaymentById(paymentId, user);

        model.addAttribute("customerName", user.getFullName());
        model.addAttribute("payment", payment);
        model.addAttribute("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));

        return "my/payment/detail";
    }
}
