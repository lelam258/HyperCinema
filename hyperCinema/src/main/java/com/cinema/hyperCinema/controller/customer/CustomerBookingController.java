package com.cinema.hyperCinema.controller.customer;

import com.cinema.hyperCinema.model.*;
import com.cinema.hyperCinema.repository.*;
import com.cinema.hyperCinema.security.CustomUserDetails;
import com.cinema.hyperCinema.service.PaymentService;
import com.cinema.hyperCinema.util.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class CustomerBookingController {

    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    @PostMapping("/my/booking/{showtimeId}/checkout")
    public String processCheckout(
            @PathVariable Integer showtimeId,
            @RequestParam("seatIds") List<Integer> seatIds,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        User user = userDetails.getUser();
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy suất chiếu ID: " + showtimeId));

        if (seatIds == null || seatIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng chọn ít nhất một ghế.");
            return "redirect:/movies/showtimes/" + showtimeId + "/seats";
        }

        // Check if seats are already booked/reserved (Booking status is not Cancelled)
        List<Ticket> activeTickets = ticketRepository.findByBooking_Showtime_ShowtimeIdAndBooking_StatusNot(showtimeId, "Cancelled");
        Set<Integer> bookedSeatIds = activeTickets.stream()
                .map(t -> t.getSeat().getSeatId())
                .collect(Collectors.toSet());

        for (Integer seatId : seatIds) {
            if (bookedSeatIds.contains(seatId)) {
                redirectAttributes.addFlashAttribute("error", "Ghế bạn chọn đã được đặt bởi người khác. Vui lòng chọn ghế khác.");
                return "redirect:/movies/showtimes/" + showtimeId + "/seats";
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

        // 1. Create Booking in "Pending" status
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setShowtime(showtime);
        booking.setTotalPrice(totalPrice);
        booking.setStatus("Pending");
        booking = bookingRepository.save(booking);

        // 2. Create Tickets in "Pending" status
        for (Seat seat : seats) {
            Ticket ticket = new Ticket();
            ticket.setBooking(booking);
            ticket.setSeat(seat);
            ticket.setQrCode("QR-" + booking.getBookingId() + "-" + seat.getSeatId());
            ticket.setStatus("Pending");
            ticketRepository.save(ticket);
        }

        // 3. Create Payment in "Pending" status
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(totalPrice);
        payment.setMethod("VNPay");
        payment.setStatus("Pending");
        paymentRepository.save(payment);

        // 4. Generate VNPay URL and redirect
        String ipAddress = VNPayUtil.getIpAddress(request);
        String vnpayUrl = paymentService.createVNPayUrl(booking, ipAddress);

        return "redirect:" + vnpayUrl;
    }

    @GetMapping("/api/payment/vnpay-return")
    public String handleVNPayCallback(
            @RequestParam Map<String, String> allRequestParams,
            RedirectAttributes redirectAttributes) {

        boolean isSuccess = paymentService.verifyAndProcessCallback(allRequestParams);
        String bookingId = allRequestParams.get("vnp_TxnRef");

        if (isSuccess) {
            return "redirect:/my/booking/result?status=success&bookingId=" + bookingId;
        } else {
            return "redirect:/my/booking/result?status=failed&bookingId=" + bookingId;
        }
    }

    @GetMapping("/my/booking/result")
    public String bookingResult(
            @RequestParam("status") String status,
            @RequestParam("bookingId") Integer bookingId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        User user = userDetails.getUser();
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Booking ID: " + bookingId));

        // Ensure current logged-in customer owns this booking
        if (!booking.getUser().getUserId().equals(user.getUserId())) {
            throw new SecurityException("Bạn không có quyền xem thông tin đặt vé này.");
        }

        model.addAttribute("booking", booking);
        model.addAttribute("status", status);
        model.addAttribute("customerName", user.getFullName());

        return "customer/movies/booking-result";
    }
}
