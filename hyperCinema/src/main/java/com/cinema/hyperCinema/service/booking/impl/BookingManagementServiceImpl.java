package com.cinema.hyperCinema.service.booking.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.hyperCinema.dto.staff.booking.BookingDetailView;
import com.cinema.hyperCinema.dto.staff.booking.BookingFoodOrderView;
import com.cinema.hyperCinema.dto.staff.booking.BookingListItemView;
import com.cinema.hyperCinema.dto.staff.booking.BookingManagementFilter;
import com.cinema.hyperCinema.dto.staff.booking.BookingManagementSummary;
import com.cinema.hyperCinema.dto.staff.booking.BookingTicketView;
import com.cinema.hyperCinema.exception.booking.BookingManagementException;
import com.cinema.hyperCinema.model.Booking;
import com.cinema.hyperCinema.model.Branch;
import com.cinema.hyperCinema.model.FoodOrder;
import com.cinema.hyperCinema.model.Payment;
import com.cinema.hyperCinema.model.Promotion;
import com.cinema.hyperCinema.model.Seat;
import com.cinema.hyperCinema.model.Showtime;
import com.cinema.hyperCinema.model.Ticket;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.repository.BookingRepository;
import com.cinema.hyperCinema.service.booking.BookingManagementService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingManagementServiceImpl implements BookingManagementService {

    private static final String STATUS_PENDING = "Pending";
    private static final String STATUS_PAID = "Paid";
    private static final String STATUS_CONFIRMED = "Confirmed";
    private static final String STATUS_CANCELLED = "Cancelled";
    private static final String STATUS_CANCELLED_UPPER = "CANCELLED";
    private static final String STATUS_SERVED = "Served";
    private static final String FOOD_STATUS_CONFIRMED = "CONFIRMED";

    private final BookingRepository bookingRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<BookingListItemView> findBookings(User actor, BookingManagementFilter filter, Pageable pageable) {
        Integer branchId = actorBranchId(actor);
        if (branchId == null) {
            return Page.empty(pageable);
        }

        BookingManagementFilter safeFilter = filter != null ? filter : new BookingManagementFilter();
        Page<Booking> bookings = bookingRepository.searchForBranchManagement(
                branchId,
                blankToNull(safeFilter.getKeyword()),
                blankToNull(safeFilter.getBookingStatus()),
                blankToNull(safeFilter.getPaymentStatus()),
                safeFilter.getMovieId(),
                startOfDay(safeFilter.getCreatedFrom()),
                dayAfter(safeFilter.getCreatedTo()),
                startOfDay(safeFilter.getShowtimeFrom()),
                dayAfter(safeFilter.getShowtimeTo()),
                pageable);

        List<BookingListItemView> rows = bookings.getContent().stream()
                .map(this::toListItem)
                .toList();
        return new PageImpl<>(rows, pageable, bookings.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public BookingManagementSummary summarize(User actor, BookingManagementFilter filter) {
        Integer branchId = actorBranchId(actor);
        if (branchId == null) {
            return BookingManagementSummary.builder().build();
        }

        BookingManagementFilter safeFilter = filter != null ? filter : new BookingManagementFilter();
        List<Object[]> results = bookingRepository.summarizeForBranchManagement(
                branchId,
                blankToNull(safeFilter.getKeyword()),
                blankToNull(safeFilter.getBookingStatus()),
                blankToNull(safeFilter.getPaymentStatus()),
                safeFilter.getMovieId(),
                startOfDay(safeFilter.getCreatedFrom()),
                dayAfter(safeFilter.getCreatedTo()),
                startOfDay(safeFilter.getShowtimeFrom()),
                dayAfter(safeFilter.getShowtimeTo()));

        if (results == null || results.isEmpty()) {
            return BookingManagementSummary.builder().build();
        }

        Object[] row = results.get(0);
        if (row == null || row.length < 4) {
            return BookingManagementSummary.builder().build();
        }

        long total = row[0] != null ? ((Number) row[0]).longValue() : 0L;
        long pending = row[1] != null ? ((Number) row[1]).longValue() : 0L;
        long paid = row[2] != null ? ((Number) row[2]).longValue() : 0L;
        long cancelled = row[3] != null ? ((Number) row[3]).longValue() : 0L;

        return BookingManagementSummary.builder()
                .total(total)
                .pending(pending)
                .paid(paid)
                .cancelled(cancelled)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BookingDetailView findDetail(User actor, Integer bookingId) {
        Booking booking = findScopedBooking(actor, bookingId);
        return toDetail(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingDetailView findCustomerDetail(User actor, Integer bookingId) {
        Booking booking = bookingRepository.findManagementDetailById(bookingId)
                .orElseThrow(() -> new BookingManagementException("booking.management.not_found"));
        Integer actorUserId = actor != null ? actor.getUserId() : null;
        Integer bookingUserId = booking.getUser() != null ? booking.getUser().getUserId() : null;
        if (!Objects.equals(actorUserId, bookingUserId)) {
            throw new BookingManagementException("booking.management.forbidden");
        }
        return toDetail(booking);
    }

    @Override
    @Transactional
    public void confirmPayment(User actor, Integer bookingId) {
        Booking booking = findScopedBooking(actor, bookingId);
        if (isCancelled(booking.getStatus()) || sameStatus(booking.getStatus(), STATUS_SERVED)) {
            throw new BookingManagementException("booking.management.invalid_status");
        }
        Payment payment = booking.getPayment();
        if (payment != null && !sameStatus(payment.getStatus(), STATUS_PENDING)) {
            throw new BookingManagementException("booking.management.payment_already_confirmed");
        }

        if (payment != null) {
            payment.setStatus(STATUS_PAID);
        }
        booking.setStatus(STATUS_PAID);
        updateTicketStatus(booking, STATUS_PAID);
    }

    @Override
    @Transactional
    public void markServed(User actor, Integer bookingId) {
        Booking booking = findScopedBooking(actor, bookingId);
        if (!sameStatus(booking.getStatus(), STATUS_PAID) && !sameStatus(booking.getStatus(), STATUS_CONFIRMED)) {
            throw new BookingManagementException("booking.management.not_paid");
        }
        booking.setStatus(STATUS_SERVED);
        updateTicketStatus(booking, STATUS_SERVED);
        if (booking.getFoodOrders() != null) {
            booking.getFoodOrders().stream()
                    .filter(order -> !isCancelled(order.getStatus()))
                    .forEach(order -> order.setStatus(FOOD_STATUS_CONFIRMED));
        }
    }

    @Override
    @Transactional
    public void cancel(User actor, Integer bookingId) {
        Booking booking = findScopedBooking(actor, bookingId);
        if (isCancelled(booking.getStatus()) || sameStatus(booking.getStatus(), STATUS_SERVED)) {
            throw new BookingManagementException("booking.management.invalid_status");
        }
        Showtime showtime = booking.getShowtime();
        if (showtime != null && showtime.getStartTime() != null
                && !showtime.getStartTime().isAfter(LocalDateTime.now())) {
            throw new BookingManagementException("booking.management.cannot_cancel_started");
        }

        booking.setStatus(STATUS_CANCELLED);
        updateTicketStatus(booking, STATUS_CANCELLED);
        Payment payment = booking.getPayment();
        if (payment != null && !sameStatus(payment.getStatus(), STATUS_PAID)) {
            payment.setStatus(STATUS_CANCELLED);
        }
        if (booking.getFoodOrders() != null) {
            booking.getFoodOrders().forEach(order -> order.setStatus(STATUS_CANCELLED_UPPER));
        }
    }

    private Booking findScopedBooking(User actor, Integer bookingId) {
        Integer branchId = actorBranchId(actor);
        if (branchId == null) {
            throw new BookingManagementException("booking.management.branch_required");
        }
        Booking booking = bookingRepository.findManagementDetailById(bookingId)
                .orElseThrow(() -> new BookingManagementException("booking.management.not_found"));
        Integer bookingBranchId = bookingBranchId(booking);
        if (!Objects.equals(branchId, bookingBranchId)) {
            throw new BookingManagementException("booking.management.forbidden");
        }
        return booking;
    }

    private Integer actorBranchId(User actor) {
        Branch branch = actor != null ? actor.getBranch() : null;
        return branch != null ? branch.getBranchId() : null;
    }

    private Integer bookingBranchId(Booking booking) {
        Showtime showtime = booking != null ? booking.getShowtime() : null;
        if (showtime == null || showtime.getHall() == null || showtime.getHall().getBranch() == null) {
            return null;
        }
        return showtime.getHall().getBranch().getBranchId();
    }

    private BookingListItemView toListItem(Booking booking) {
        Showtime showtime = booking.getShowtime();
        Payment payment = booking.getPayment();
        List<Ticket> tickets = booking.getTickets() != null ? booking.getTickets() : List.of();
        return BookingListItemView.builder()
                .bookingId(booking.getBookingId())
                .customerName(booking.getUser() != null ? booking.getUser().getFullName() : "Guest")
                .customerEmail(booking.getUser() != null ? booking.getUser().getEmail() : "")
                .movieTitle(showtime != null && showtime.getMovie() != null ? showtime.getMovie().getTitle() : "N/A")
                .branchName(showtime != null && showtime.getHall() != null && showtime.getHall().getBranch() != null
                        ? showtime.getHall().getBranch().getName() : "N/A")
                .hallName(showtime != null && showtime.getHall() != null ? showtime.getHall().getName() : "N/A")
                .showtimeStart(showtime != null ? showtime.getStartTime() : null)
                .createdAt(booking.getCreatedAt())
                .totalPrice(booking.getTotalPrice())
                .bookingStatus(booking.getStatus())
                .paymentStatus(payment != null ? payment.getStatus() : "")
                .paymentMethod(payment != null ? payment.getMethod() : "")
                .seats(tickets.stream().map(ticket -> seatLabel(ticket.getSeat())).toList())
                .ticketCount(tickets.size())
                .build();
    }

    private BookingDetailView toDetail(Booking booking) {
        BookingListItemView listItem = toListItem(booking);
        Showtime showtime = booking.getShowtime();
        Payment payment = booking.getPayment();
        Promotion promotion = booking.getPromotion();
        return BookingDetailView.builder()
                .bookingId(booking.getBookingId())
                .customerName(listItem.getCustomerName())
                .customerEmail(listItem.getCustomerEmail())
                .customerPhone(booking.getUser() != null ? booking.getUser().getPhone() : "")
                .movieTitle(listItem.getMovieTitle())
                .branchName(listItem.getBranchName())
                .hallName(listItem.getHallName())
                .showtimeStart(showtime != null ? showtime.getStartTime() : null)
                .showtimeEnd(showtime != null ? showtime.getEndTime() : null)
                .createdAt(booking.getCreatedAt())
                .seatSubtotal(booking.getSeatSubtotal())
                .foodSubtotal(booking.getFoodSubtotal())
                .orderSubtotal(booking.getOrderSubtotal())
                .voucherDiscountAmount(booking.getVoucherDiscountAmount())
                .membershipDiscountBase(membershipDiscountBase(booking))
                .membershipDiscountAmount(booking.getMembershipDiscountAmount())
                .membershipPlanName(booking.getMembershipPlanName())
                .membershipDiscountPercent(booking.getMembershipDiscountPercent())
                .totalPrice(booking.getTotalPrice())
                .bookingStatus(booking.getStatus())
                .paymentStatus(payment != null ? payment.getStatus() : "")
                .paymentMethod(payment != null ? payment.getMethod() : "")
                .paymentAmount(payment != null ? payment.getAmount() : null)
                .promotionName(promotion != null ? promotion.getCode() : "")
                .tickets(toTicketViews(booking))
                .foodOrders(toFoodOrderViews(booking))
                .canConfirmPayment(canConfirmPayment(booking))
                .canMarkServed(canMarkServed(booking))
                .canCancel(canCancel(booking))
                .build();
    }

    private List<BookingTicketView> toTicketViews(Booking booking) {
        if (booking.getTickets() == null) {
            return List.of();
        }
        return booking.getTickets().stream()
                .map(ticket -> BookingTicketView.builder()
                        .ticketId(ticket.getTicketId())
                        .seatLabel(seatLabel(ticket.getSeat()))
                        .seatType(ticket.getSeat() != null ? ticket.getSeat().getType() : "")
                        .qrCode(ticket.getQrCode())
                        .status(ticket.getStatus())
                        .build())
                .toList();
    }

    private long membershipDiscountBase(Booking booking) {
        long orderSubtotal = booking.getOrderSubtotal() != null
                ? booking.getOrderSubtotal()
                : (booking.getTotalPrice() != null ? booking.getTotalPrice() : 0L);
        long voucherDiscount = booking.getVoucherDiscountAmount() != null ? booking.getVoucherDiscountAmount() : 0L;
        return Math.max(0L, orderSubtotal - voucherDiscount);
    }

    private List<BookingFoodOrderView> toFoodOrderViews(Booking booking) {
        if (booking.getFoodOrders() == null) {
            return List.of();
        }
        return booking.getFoodOrders().stream()
                .map(order -> BookingFoodOrderView.builder()
                        .orderId(order.getOrderId())
                        .status(order.getStatus())
                        .totalAmount(order.getTotalAmount())
                        .createdAt(order.getCreatedAt())
                        .build())
                .toList();
    }

    private boolean canConfirmPayment(Booking booking) {
        Payment payment = booking.getPayment();
        return !isCancelled(booking.getStatus())
                && !sameStatus(booking.getStatus(), STATUS_SERVED)
                && (payment == null || sameStatus(payment.getStatus(), STATUS_PENDING));
    }

    private boolean canMarkServed(Booking booking) {
        return sameStatus(booking.getStatus(), STATUS_PAID) || sameStatus(booking.getStatus(), STATUS_CONFIRMED);
    }

    private boolean canCancel(Booking booking) {
        Showtime showtime = booking.getShowtime();
        return !isCancelled(booking.getStatus())
                && !sameStatus(booking.getStatus(), STATUS_SERVED)
                && (showtime == null || showtime.getStartTime() == null
                        || showtime.getStartTime().isAfter(LocalDateTime.now()));
    }

    private void updateTicketStatus(Booking booking, String status) {
        if (booking.getTickets() != null) {
            booking.getTickets().forEach(ticket -> ticket.setStatus(status));
        }
    }

    private static String seatLabel(Seat seat) {
        if (seat == null) {
            return "N/A";
        }
        return seat.getSeatRow() + seat.getSeatNumber();
    }

    private static boolean sameStatus(String actual, String expected) {
        return normalize(actual).equals(normalize(expected));
    }

    private static boolean isCancelled(String status) {
        return sameStatus(status, STATUS_CANCELLED) || sameStatus(status, STATUS_CANCELLED_UPPER);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static LocalDateTime startOfDay(LocalDate date) {
        return date != null ? date.atStartOfDay() : null;
    }

    private static LocalDateTime dayAfter(LocalDate date) {
        return date != null ? date.plusDays(1).atStartOfDay() : null;
    }
}
