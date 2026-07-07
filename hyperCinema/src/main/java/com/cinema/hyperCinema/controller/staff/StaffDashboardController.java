package com.cinema.hyperCinema.controller.staff;

import com.cinema.hyperCinema.dto.admin.seat.response.ShowtimeSeatView;
import com.cinema.hyperCinema.model.*;
import com.cinema.hyperCinema.repository.*;
import com.cinema.hyperCinema.security.CustomUserDetails;
import com.cinema.hyperCinema.service.seat.SeatService;
import com.cinema.hyperCinema.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffDashboardController {

    private final ShowtimeRepository showtimeRepository;
    private final SeatService seatService;
    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    @GetMapping({"/dashboard", ""})
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User user = userDetails.getUser();
        
        if (user.getBranch() != null) {
            model.addAttribute("branchName", user.getBranch().getName());
        } else {
            model.addAttribute("branchName", "Chưa phân công chi nhánh");
        }
        
        model.addAttribute("staffName", user.getFullName());

        // ── Meta ──
        model.addAttribute("lastUpdated",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));

        return "staff/dashboard";
    }

    @GetMapping("/booking")
    public String bookingList(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {
        User user = userDetails.getUser();
        
        Integer branchId = user.getBranch() != null ? user.getBranch().getBranchId() : null;
        Pageable pageable = PageRequest.of(page, size);
        Page<Showtime> showtimes = Page.empty(pageable);
        
        if (branchId != null) {
            showtimes = showtimeRepository.findByHall_Branch_BranchIdAndStartTimeAfterOrderByStartTimeAsc(branchId, LocalDateTime.now(), pageable);
            model.addAttribute("branchName", user.getBranch().getName());
        } else {
            model.addAttribute("branchName", "Chưa phân công chi nhánh");
        }
        
        model.addAttribute("staffName", user.getFullName());
        model.addAttribute("showtimes", showtimes);
        
        model.addAttribute("lastUpdated",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));
                
        return "staff/booking";
    }

    @GetMapping("/booking/{showtimeId}")
    public String showtimeBookingSeats(@PathVariable Integer showtimeId,
                                      @AuthenticationPrincipal CustomUserDetails userDetails,
                                      Model model) {
        User user = userDetails.getUser();
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy suất chiếu ID: " + showtimeId));

        // Kiểm tra quyền: Nhân viên chỉ bán vé cho suất chiếu tại chi nhánh của mình
        if (user.getBranch() == null || showtime.getHall().getBranch() == null ||
            !user.getBranch().getBranchId().equals(showtime.getHall().getBranch().getBranchId())) {
            throw new SecurityException("Bạn không thể bán vé cho suất chiếu ở chi nhánh khác.");
        }

        List<ShowtimeSeatView> seats = seatService.getSeatsForShowtime(showtimeId);

        model.addAttribute("showtime", showtime);
        model.addAttribute("seats", seats);
        model.addAttribute("branchName", user.getBranch().getName());
        model.addAttribute("staffName", user.getFullName());
        model.addAttribute("lastUpdated",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));

        return "staff/booking-detail";
    }

    @PostMapping("/booking/{showtimeId}/checkout")
    public String processCheckout(@PathVariable Integer showtimeId,
                                  @RequestParam("seatIds") List<Integer> seatIds,
                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {
        User user = userDetails.getUser();
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy suất chiếu ID: " + showtimeId));

        if (user.getBranch() == null || showtime.getHall().getBranch() == null ||
            !user.getBranch().getBranchId().equals(showtime.getHall().getBranch().getBranchId())) {
            throw new SecurityException("Bạn không thể bán vé cho suất chiếu ở chi nhánh khác.");
        }

        if (seatIds == null || seatIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorKey", "booking.seats.empty");
            return "redirect:/staff/booking/" + showtimeId;
        }

        // Kiểm tra xem ghế đã bị đặt chưa
        List<Ticket> activeTickets = ticketRepository.findByBooking_Showtime_ShowtimeIdAndBooking_StatusNot(showtimeId, "Cancelled");
        Set<Integer> bookedSeatIds = activeTickets.stream()
                .map(t -> t.getSeat().getSeatId())
                .collect(Collectors.toSet());

        for (Integer seatId : seatIds) {
            if (bookedSeatIds.contains(seatId)) {
                redirectAttributes.addFlashAttribute("errorKey", "booking.seats.already_booked");
                return "redirect:/staff/booking/" + showtimeId;
            }
        }

        List<Seat> seats = seatRepository.findAllById(seatIds);
        long totalPrice = 0;
        for (Seat seat : seats) {
            long seatPrice = showtime.getPrice();
            if ("VIP".equalsIgnoreCase(seat.getType())) {
                seatPrice += 20000;
            } else if ("Double".equalsIgnoreCase(seat.getType())) {
                seatPrice = showtime.getPrice() * 2;
            }
            totalPrice += seatPrice;
        }

        // Tạo Booking
        Booking booking = new Booking();
        booking.setUser(user); // Gán người mua/tác nhân là staff bán tại quầy
        booking.setShowtime(showtime);
        booking.setTotalPrice(totalPrice);
        booking.setStatus("Completed");
        booking = bookingRepository.save(booking);

        // Tạo Tickets
        for (Seat seat : seats) {
            Ticket ticket = new Ticket();
            ticket.setBooking(booking);
            ticket.setSeat(seat);
            ticket.setQrCode("QR-" + booking.getBookingId() + "-" + seat.getSeatId());
            ticket.setStatus("Active");
            ticketRepository.save(ticket);
        }

        // Tạo Payment bằng tiền mặt
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(totalPrice);
        payment.setMethod("Cash");
        payment.setStatus("Completed");
        paymentRepository.save(payment);

        redirectAttributes.addFlashAttribute("successKey", "booking.checkout.success");
        return "redirect:/staff/booking";
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

        if (user.getBranch() != null) {
            model.addAttribute("branchName", user.getBranch().getName());
        } else {
            model.addAttribute("branchName", "Chưa phân công chi nhánh");
        }

        model.addAttribute("staffName", user.getFullName());
        model.addAttribute("payments", paymentPage);
        model.addAttribute("status", status);
        model.addAttribute("method", method);
        model.addAttribute("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));

        return "staff/payment/list";
    }

    @GetMapping("/payments/{paymentId}")
    public String paymentDetail(
            @PathVariable Integer paymentId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {
        User user = userDetails.getUser();
        Payment payment = paymentService.getPaymentById(paymentId, user);

        if (user.getBranch() != null) {
            model.addAttribute("branchName", user.getBranch().getName());
        } else {
            model.addAttribute("branchName", "Chưa phân công chi nhánh");
        }

        model.addAttribute("staffName", user.getFullName());
        model.addAttribute("payment", payment);
        model.addAttribute("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));

        return "staff/payment/detail";
    }
}
