package com.cinema.hyperCinema.service.booking.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.hyperCinema.dto.admin.voucher.response.VoucherPreview;
import com.cinema.hyperCinema.model.Booking;
import com.cinema.hyperCinema.model.FoodItem;
import com.cinema.hyperCinema.model.FoodOrder;
import com.cinema.hyperCinema.model.FoodOrderItem;
import com.cinema.hyperCinema.model.Payment;
import com.cinema.hyperCinema.model.Seat;
import com.cinema.hyperCinema.model.Showtime;
import com.cinema.hyperCinema.model.Ticket;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.repository.BookingRepository;
import com.cinema.hyperCinema.repository.FoodItemRepository;
import com.cinema.hyperCinema.repository.FoodOrderItemRepository;
import com.cinema.hyperCinema.repository.FoodOrderRepository;
import com.cinema.hyperCinema.repository.PaymentRepository;
import com.cinema.hyperCinema.repository.SeatRepository;
import com.cinema.hyperCinema.repository.SeatReservationRepository;
import com.cinema.hyperCinema.repository.ShowtimeRepository;
import com.cinema.hyperCinema.repository.TicketRepository;
import com.cinema.hyperCinema.repository.UserRepository;
import com.cinema.hyperCinema.service.booking.BookingService;
import com.cinema.hyperCinema.service.voucher.VoucherApplicationService;
import com.cinema.hyperCinema.util.SeatPricing;

@Service
public class BookingServiceImpl implements BookingService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_PENDING = "Pending";
    private static final String STATUS_CONFIRMED = "Confirmed";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_CANCELLED_LEGACY = "Cancelled";
    private static final String MAINTENANCE = "UNDER_MAINTENANCE";
    private static final String PAYMENT_PENDING = "Pending";
    private static final String PAYMENT_COMPLETED = "Completed";
    private static final String PAYMENT_METHOD_VIETQR = "VietQR";
    private static final String PAYMENT_METHOD_CASH = "Cash";
    private static final String PAYMENT_METHOD_CARD = "Card";
    private static final String FOOD_ORDER_PENDING = "PENDING";
    private static final String FOOD_ORDER_CONFIRMED = "CONFIRMED";
    private static final String CUSTOMER_ROLE = "Customer";

    private final BookingRepository bookingRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final TicketRepository ticketRepository;
    private final SeatReservationRepository seatReservationRepository;
    private final PaymentRepository paymentRepository;
    private final FoodItemRepository foodItemRepository;
    private final FoodOrderRepository foodOrderRepository;
    private final FoodOrderItemRepository foodOrderItemRepository;
    private final UserRepository userRepository;
    private final VoucherApplicationService voucherApplicationService;
    private final long paymentTimeoutMinutes;

    public BookingServiceImpl(BookingRepository bookingRepository,
                              ShowtimeRepository showtimeRepository,
                              SeatRepository seatRepository,
                              TicketRepository ticketRepository,
                              SeatReservationRepository seatReservationRepository,
                              PaymentRepository paymentRepository,
                              FoodItemRepository foodItemRepository,
                              FoodOrderRepository foodOrderRepository,
                              FoodOrderItemRepository foodOrderItemRepository,
                              UserRepository userRepository,
                              VoucherApplicationService voucherApplicationService,
                              @Value("${booking.payment.timeout-minutes:15}") long paymentTimeoutMinutes) {
        this.bookingRepository = bookingRepository;
        this.showtimeRepository = showtimeRepository;
        this.seatRepository = seatRepository;
        this.ticketRepository = ticketRepository;
        this.seatReservationRepository = seatReservationRepository;
        this.paymentRepository = paymentRepository;
        this.foodItemRepository = foodItemRepository;
        this.foodOrderRepository = foodOrderRepository;
        this.foodOrderItemRepository = foodOrderItemRepository;
        this.userRepository = userRepository;
        this.voucherApplicationService = voucherApplicationService;
        this.paymentTimeoutMinutes = paymentTimeoutMinutes;
    }

    @Override
    public List<Booking> findRecentBookingsByUser(Integer userId, int limit) {
        return bookingRepository.findByUser_UserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, limit));
    }

    @Override
    public Optional<Booking> findById(Integer bookingId) {
        return bookingRepository.findById(bookingId);
    }

    @Override
    public Booking save(Booking booking) {
        return bookingRepository.save(booking);
    }

    @Override
    public Optional<Showtime> findShowtimeWithDetails(Integer showtimeId) {
        return showtimeRepository.findByIdWithMovieHallAndBranch(showtimeId);
    }

    @Override
    public List<Showtime> findUpcomingShowtimesForMovie(Integer movieId) {
        return showtimeRepository.findUpcomingByMovieIdWithHallAndBranch(
                movieId, LocalDateTime.now().minusMinutes(1));
    }

    @Override
    @Transactional
    public Booking createPendingVietQrBooking(User user,
                                              Integer showtimeId,
                                              List<Integer> seatIds,
                                              List<Integer> foodItemIds,
                                              List<Integer> foodQuantities) {
        return createPosBooking(user, showtimeId, seatIds, foodItemIds, foodQuantities,
                PAYMENT_METHOD_VIETQR, null, null);
    }

    @Override
    @Transactional
    public Booking createPosBooking(User actor,
                                    Integer showtimeId,
                                    List<Integer> seatIds,
                                    List<Integer> foodItemIds,
                                    List<Integer> foodQuantities,
                                    String paymentMethod,
                                    String voucherCode,
                                    String customerPhone) {
        if (seatIds == null || seatIds.isEmpty()) {
            throw new IllegalArgumentException("Vui long chon it nhat mot ghe.");
        }

        Showtime showtime = findShowtimeWithDetails(showtimeId)
                .orElseThrow(() -> new IllegalStateException("Suat chieu khong ton tai."));
        assertCanBookShowtime(actor, showtime);

        List<Seat> seats = seatRepository.findAllById(seatIds);
        if (seats.size() != seatIds.size()
                || seats.stream().anyMatch(seat -> !seat.getHall().getHallId().equals(showtime.getHall().getHallId()))
                || seats.stream().anyMatch(seat -> MAINTENANCE.equals(seat.getMaintenanceStatus()))
                || hasUnavailableSeats(showtimeId, seatIds)) {
            throw new IllegalArgumentException("Mot so ghe vua duoc dat. Vui long chon lai.");
        }

        String normalizedPaymentMethod = normalizePaymentMethod(paymentMethod);
        if (!isOperationalUser(actor)) {
            normalizedPaymentMethod = PAYMENT_METHOD_VIETQR;
        }
        List<FoodSelection> foodSelections = selectedFood(foodItemIds, foodQuantities);
        long seatTotal = seats.stream()
                .mapToLong(seat -> SeatPricing.priceFor(seat.getType()))
                .sum();
        long foodTotal = foodSelections.stream()
                .mapToLong(selection -> (long) selection.item().getPrice() * selection.quantity())
                .sum();
        long subtotal = seatTotal + foodTotal;
        VoucherPreview voucherPreview = previewVoucher(voucherCode, subtotal, showtime);
        long discount = voucherPreview != null ? voucherPreview.getDiscountAmount() : 0L;
        long total = Math.max(0L, subtotal - discount);
        boolean payImmediately = isImmediateCounterPayment(normalizedPaymentMethod);

        Booking booking = new Booking();
        booking.setUser(resolveBookingUser(actor, customerPhone));
        booking.setShowtime(showtime);
        booking.setStatus(payImmediately ? STATUS_CONFIRMED : STATUS_PENDING);
        booking.setTotalPrice(total);

        List<Ticket> tickets = seats.stream()
                .map(seat -> {
                    Ticket ticket = new Ticket();
                    ticket.setBooking(booking);
                    ticket.setSeat(seat);
                    ticket.setStatus(payImmediately ? STATUS_ACTIVE : STATUS_PENDING);
                    ticket.setQrCode("HC-" + showtimeId + "-" + seat.getSeatId() + "-" + System.currentTimeMillis());
                    return ticket;
                })
                .collect(Collectors.toList());
        booking.setTickets(tickets);
        Booking savedBooking = bookingRepository.save(booking);
        createFoodOrder(savedBooking, foodSelections, payImmediately);
        if (voucherPreview != null) {
            voucherApplicationService.apply(voucherPreview.getCode(), savedBooking, LocalDateTime.now());
        }

        Payment payment = new Payment();
        payment.setBooking(savedBooking);
        payment.setAmount(savedBooking.getTotalPrice());
        payment.setMethod(normalizedPaymentMethod);
        payment.setStatus(payImmediately ? PAYMENT_COMPLETED : PAYMENT_PENDING);
        payment.setExpiresAt(payImmediately ? null : LocalDateTime.now().plusMinutes(paymentTimeoutMinutes));
        paymentRepository.save(payment);

        return savedBooking;
    }

    private List<FoodSelection> selectedFood(List<Integer> foodItemIds, List<Integer> foodQuantities) {
        if (foodItemIds == null || foodItemIds.isEmpty()) {
            return List.of();
        }
        if (foodQuantities == null || foodItemIds.size() != foodQuantities.size()) {
            throw new IllegalArgumentException("Thong tin F&B khong hop le.");
        }

        List<FoodSelection> selections = new ArrayList<>();
        for (int i = 0; i < foodItemIds.size(); i++) {
            Integer itemId = foodItemIds.get(i);
            Integer quantity = foodQuantities.get(i);
            if (itemId == null || quantity == null || quantity <= 0) {
                continue;
            }

            FoodItem item = foodItemRepository.findById(itemId)
                    .orElseThrow(() -> new IllegalArgumentException("San pham F&B khong ton tai."));
            if (!Boolean.TRUE.equals(item.getIsAvailable())) {
                throw new IllegalArgumentException("Mot san pham F&B hien khong con ban.");
            }
            if (item.getStock() == null || item.getStock() < quantity) {
                throw new IllegalArgumentException("Mot san pham F&B khong du ton kho.");
            }
            selections.add(new FoodSelection(item, quantity));
        }
        return selections;
    }

    private void createFoodOrder(Booking booking, List<FoodSelection> foodSelections, boolean confirmed) {
        if (foodSelections.isEmpty()) {
            return;
        }

        FoodOrder order = new FoodOrder();
        order.setBooking(booking);
        order.setStatus(confirmed ? FOOD_ORDER_CONFIRMED : FOOD_ORDER_PENDING);
        order.setTotalAmount(foodSelections.stream()
                .mapToInt(selection -> selection.item().getPrice() * selection.quantity())
                .sum());
        FoodOrder savedOrder = foodOrderRepository.save(order);

        List<FoodOrderItem> orderItems = foodSelections.stream()
                .map(selection -> {
                    FoodOrderItem orderItem = new FoodOrderItem();
                    orderItem.setOrderId(savedOrder.getOrderId());
                    orderItem.setItemId(selection.item().getItemId());
                    orderItem.setQuantity(selection.quantity());
                    orderItem.setUnitPrice(selection.item().getPrice());
                    return orderItem;
                })
                .collect(Collectors.toList());
        foodOrderItemRepository.saveAll(orderItems);
        if (confirmed) {
            foodSelections.forEach(selection -> {
                FoodItem item = selection.item();
                item.setStock(Math.max(0, item.getStock() - selection.quantity()));
                foodItemRepository.save(item);
            });
        }
    }

    private VoucherPreview previewVoucher(String voucherCode, long subtotal, Showtime showtime) {
        String normalizedCode = voucherCode != null ? voucherCode.trim() : "";
        if (normalizedCode.isBlank()) {
            return null;
        }
        Integer branchId = showtime.getHall() != null && showtime.getHall().getBranch() != null
                ? showtime.getHall().getBranch().getBranchId()
                : null;
        VoucherPreview preview = voucherApplicationService.validateAndPreview(
                normalizedCode, subtotal, branchId, LocalDateTime.now());
        if (!preview.isValid()) {
            throw new IllegalArgumentException("Voucher khong hop le hoac khong du dieu kien ap dung.");
        }
        return preview;
    }

    private User resolveBookingUser(User actor, String customerPhone) {
        if (!isOperationalUser(actor)) {
            return actor;
        }
        String normalizedPhone = customerPhone != null ? customerPhone.trim() : "";
        if (normalizedPhone.isBlank()) {
            return actor;
        }
        return userRepository.findByPhoneAndRoleName(normalizedPhone, CUSTOMER_ROLE).orElse(actor);
    }

    private String normalizePaymentMethod(String paymentMethod) {
        String method = paymentMethod != null ? paymentMethod.trim().toUpperCase() : "";
        return switch (method) {
            case "CASH", "TIEN_MAT" -> PAYMENT_METHOD_CASH;
            case "CARD", "QUET_THE" -> PAYMENT_METHOD_CARD;
            case "QR", "QR_CODE", "VIETQR" -> PAYMENT_METHOD_VIETQR;
            default -> throw new IllegalArgumentException("Vui long chon phuong thuc thanh toan hop le.");
        };
    }

    private boolean isImmediateCounterPayment(String paymentMethod) {
        return PAYMENT_METHOD_CASH.equals(paymentMethod) || PAYMENT_METHOD_CARD.equals(paymentMethod);
    }

    private void assertCanBookShowtime(User actor, Showtime showtime) {
        if (actor == null || !isOperationalUser(actor)) {
            return;
        }
        if (actor.getBranch() == null) {
            throw new IllegalStateException("Nhan vien chua duoc gan chi nhanh.");
        }
        Integer actorBranchId = actor.getBranch().getBranchId();
        Integer showtimeBranchId = showtime.getHall() != null && showtime.getHall().getBranch() != null
                ? showtime.getHall().getBranch().getBranchId()
                : null;
        if (!actorBranchId.equals(showtimeBranchId)) {
            throw new IllegalStateException("Suat chieu khong thuoc chi nhanh cua nhan vien.");
        }
    }

    private boolean isOperationalUser(User actor) {
        if (actor == null || actor.getRole() == null || actor.getRole().getName() == null) {
            return false;
        }
        String roleName = actor.getRole().getName();
        return "Staff".equalsIgnoreCase(roleName)
                || "Manager".equalsIgnoreCase(roleName)
                || "BranchManager".equalsIgnoreCase(roleName);
    }

    private boolean hasUnavailableSeats(Integer showtimeId, List<Integer> seatIds) {
        Set<Integer> requestedSeatIds = Set.copyOf(seatIds);
        List<Integer> bookedSeatIds = ticketRepository.findUnavailableSeatIdsByShowtimeId(
                showtimeId, Arrays.asList(STATUS_CANCELLED, STATUS_CANCELLED_LEGACY));
        if (bookedSeatIds.stream().anyMatch(requestedSeatIds::contains)) {
            return true;
        }
        List<Integer> reservedSeatIds = seatReservationRepository.findActiveReservedSeatIds(
                showtimeId, STATUS_ACTIVE, LocalDateTime.now());
        return reservedSeatIds.stream().anyMatch(requestedSeatIds::contains);
    }

    private record FoodSelection(FoodItem item, Integer quantity) {}
}
