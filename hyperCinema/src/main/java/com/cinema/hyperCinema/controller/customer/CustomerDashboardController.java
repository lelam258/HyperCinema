package com.cinema.hyperCinema.controller.customer;

import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.model.Booking;
import com.cinema.hyperCinema.model.Payment;
import com.cinema.hyperCinema.model.Notification;
import com.cinema.hyperCinema.repository.BookingRepository;
import com.cinema.hyperCinema.security.CustomUserDetails;
import com.cinema.hyperCinema.service.PaymentService;
import com.cinema.hyperCinema.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/my")
public class CustomerDashboardController {

    private final BookingRepository bookingRepository;
    private final PaymentService paymentService;
    private final NotificationService notificationService;

    public CustomerDashboardController(BookingRepository bookingRepository, 
                                       PaymentService paymentService, 
                                       NotificationService notificationService) {
        this.bookingRepository = bookingRepository;
        this.paymentService = paymentService;
        this.notificationService = notificationService;
    }

    @ModelAttribute
    public void addCommonAttributes(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails != null) {
            User user = userDetails.getUser();
            long unreadCount = notificationService.countUnreadNotifications(user);
            model.addAttribute("unreadNotificationCount", unreadCount);
        }
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
    public String bookingHistory(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {
        User user = userDetails.getUser();
        Pageable pageable = PageRequest.of(page, size);
        Page<Booking> bookings = bookingRepository.findByUser_UserIdOrderByCreatedAtDesc(user.getUserId(), pageable);
        
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

    @GetMapping("/notifications")
    public String notifications(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User user = userDetails.getUser();
        List<Notification> notifications = notificationService.getReceivedNotifications(user);
        
        model.addAttribute("customerName", user.getFullName());
        model.addAttribute("notifications", notifications);
        model.addAttribute("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));
        
        return "my/notifications";
    }

    @PostMapping("/notifications/{id}/read")
    @ResponseBody
    public ResponseEntity<?> markNotificationAsRead(
            @PathVariable("id") Integer id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            User user = userDetails.getUser();
            Notification notification = notificationService.markAsRead(id, user);
            long unreadCount = notificationService.countUnreadNotifications(user);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "unreadCount", unreadCount
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
