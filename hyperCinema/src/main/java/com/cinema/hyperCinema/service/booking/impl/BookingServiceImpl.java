package com.cinema.hyperCinema.service.booking.impl;

import com.cinema.hyperCinema.dto.admin.voucher.response.VoucherPreview;
import com.cinema.hyperCinema.model.*;
import com.cinema.hyperCinema.repository.*;
import com.cinema.hyperCinema.service.booking.BookingPricingResult;
import com.cinema.hyperCinema.service.booking.BookingService;
import com.cinema.hyperCinema.service.pricing.HallSeatTypePricingService;
import com.cinema.hyperCinema.service.voucher.VoucherApplicationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BookingServiceImpl implements BookingService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_PENDING = "Pending";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_CANCELLED_LEGACY = "Cancelled";
    private static final String MAINTENANCE = "UNDER_MAINTENANCE";
    private static final String SHOWTIME_CANCELLED = "CANCELLED";
    private static final String PAYMENT_PENDING = "Pending";
    private static final String PAYMENT_METHOD_VNPAY = "VNPay";
    private static final String FOOD_ORDER_PENDING = "PENDING";
    private static final String ROLE_CUSTOMER = "Customer";

    private final BookingRepository bookingRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final TicketRepository ticketRepository;
    private final SeatReservationRepository seatReservationRepository;
    private final PaymentRepository paymentRepository;
    private final FoodItemRepository foodItemRepository;
    private final FoodOrderRepository foodOrderRepository;
    private final FoodOrderItemRepository foodOrderItemRepository;
    private final UserMembershipRepository userMembershipRepository;
    private final VoucherApplicationService voucherApplicationService;
    private final HallSeatTypePricingService hallSeatTypePricingService;
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
                              UserMembershipRepository userMembershipRepository,
                              VoucherApplicationService voucherApplicationService,
                              HallSeatTypePricingService hallSeatTypePricingService,
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
        this.userMembershipRepository = userMembershipRepository;
        this.voucherApplicationService = voucherApplicationService;
        this.hallSeatTypePricingService = hallSeatTypePricingService;
        this.paymentTimeoutMinutes = paymentTimeoutMinutes;
    }

    @Override
    public List<Booking> findRecentBookingsByUser(Integer userId, int limit) {
        return findBookingsByUser(userId, PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
    }

    @Override
    public Page<Booking> findBookingsByUser(Integer userId, Pageable pageable) {
        Page<Integer> bookingIds = bookingRepository.findBookingIdsByUserId(userId, pageable);
        if (bookingIds.isEmpty()) {
            return Page.empty(pageable);
        }

        Map<Integer, Booking> bookingsById = bookingRepository.findListDetailsByBookingIdIn(bookingIds.getContent())
                .stream()
                .collect(Collectors.toMap(Booking::getBookingId, booking -> booking));
        List<Booking> orderedBookings = bookingIds.getContent().stream()
                .map(bookingsById::get)
                .filter(Objects::nonNull)
                .toList();
        return new org.springframework.data.domain.PageImpl<>(orderedBookings, pageable, bookingIds.getTotalElements());
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
    public Booking createPendingVNPayBooking(User user,
                                             Integer showtimeId,
                                             List<Integer> seatIds,
                                             List<Integer> foodItemIds,
                                             List<Integer> foodQuantities,
                                             String voucherCode) {
        return createPendingBooking(user, showtimeId, seatIds, foodItemIds, foodQuantities,
                voucherCode);
    }

    private Booking createPendingBooking(User user,
                                         Integer showtimeId,
                                         List<Integer> seatIds,
                                         List<Integer> foodItemIds,
                                         List<Integer> foodQuantities,
                                         String voucherCode) {
        if (seatIds == null || seatIds.isEmpty()) {
            throw new IllegalArgumentException("Vui long chon it nhat mot ghe.");
        }

        Showtime showtime = findShowtimeWithDetails(showtimeId)
                .orElseThrow(() -> new IllegalStateException("Suat chieu khong ton tai."));

        if (SHOWTIME_CANCELLED.equals(showtime.getStatus())) {
            throw new IllegalStateException("Suat chieu khong ton tai.");
        }

        List<Seat> seats = seatRepository.findAllById(seatIds);
        if (seats.size() != seatIds.size()
                || seats.stream().anyMatch(seat -> !seat.getHall().getHallId().equals(showtime.getHall().getHallId()))
                || seats.stream().anyMatch(seat -> MAINTENANCE.equals(seat.getMaintenanceStatus()))
                || hasUnavailableSeats(showtimeId, seatIds)) {
            throw new IllegalArgumentException("Ghe da duoc dat. Vui long chon lai.");
        }

        List<FoodSelection> foodSelections = selectedFood(foodItemIds, foodQuantities);
        long seatTotal = seats.stream()
                .mapToLong(seat -> hallSeatTypePricingService.priceForSeat(showtime.getHall(), seat))
                .sum();
        long foodTotal = foodSelections.stream()
                .mapToLong(selection -> (long) selection.item().getPrice() * selection.quantity())
                .sum();
        LocalDateTime now = LocalDateTime.now();
        BookingPricingResult pricing = calculatePricing(user, showtime, seatTotal, foodTotal, voucherCode, now);

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setShowtime(showtime);
        booking.setStatus(STATUS_PENDING);
        booking.setSeatSubtotal(pricing.seatSubtotal());
        booking.setFoodSubtotal(pricing.foodSubtotal());
        booking.setOrderSubtotal(pricing.orderSubtotal());
        booking.setVoucherDiscountAmount(pricing.voucherDiscountAmount());
        booking.setMembershipDiscountAmount(pricing.membershipDiscountAmount());
        booking.setMembershipPlanName(pricing.membershipPlanName());
        booking.setMembershipDiscountPercent(pricing.membershipDiscountPercent());
        booking.setTotalPrice(pricing.finalTotal());

        List<Ticket> tickets = seats.stream()
                .map(seat -> {
                    Ticket ticket = new Ticket();
                    ticket.setBooking(booking);
                    ticket.setSeat(seat);
                    ticket.setStatus(STATUS_PENDING);
                    ticket.setQrCode("HC-" + showtimeId + "-" + seat.getSeatId() + "-" + System.currentTimeMillis());
                    return ticket;
                })
                .collect(Collectors.toList());
        booking.setTickets(tickets);
        Booking savedBooking = bookingRepository.save(booking);

        if (pricing.voucherCode() != null) {
            Promotion promotion = voucherApplicationService.applyValidated(
                    pricing.voucherCode(), savedBooking, pricing.orderSubtotal(), branchId(showtime), now);
            savedBooking.setPromotion(promotion);
            savedBooking = bookingRepository.save(savedBooking);
        }

        createPendingFoodOrder(savedBooking, foodSelections);

        Payment payment = new Payment();
        payment.setBooking(savedBooking);
        payment.setAmount(savedBooking.getTotalPrice());
        payment.setMethod(PAYMENT_METHOD_VNPAY);
        payment.setStatus(PAYMENT_PENDING);
        payment.setExpiresAt(now.plusMinutes(paymentTimeoutMinutes));
        payment = paymentRepository.save(payment);
        savedBooking.setPayment(payment);

        return savedBooking;
    }

    private BookingPricingResult calculatePricing(User user,
                                                  Showtime showtime,
                                                  long seatTotal,
                                                  long foodTotal,
                                                  String voucherCode,
                                                  LocalDateTime now) {
        long orderSubtotal = Math.max(0, seatTotal + foodTotal);
        String normalizedVoucherCode = normalizeVoucherCode(voucherCode);
        long voucherDiscount = 0L;
        if (normalizedVoucherCode != null) {
            VoucherPreview preview = voucherApplicationService.validateAndPreview(
                    normalizedVoucherCode, orderSubtotal, branchId(showtime), now);
            if (!preview.isValid()) {
                throw new IllegalArgumentException(preview.getErrorKey());
            }
            voucherDiscount = clamp(preview.getDiscountAmount(), 0, orderSubtotal);
        }

        long afterVoucher = Math.max(0, orderSubtotal - voucherDiscount);
        UserMembership membership = activeMembership(user, now.toLocalDate()).orElse(null);
        String membershipPlanName = null;
        BigDecimal membershipPercent = null;
        long membershipDiscount = 0L;
        if (membership != null && membership.getPlan() != null
                && membership.getPlan().getDiscountPercent() != null
                && membership.getPlan().getDiscountPercent().compareTo(BigDecimal.ZERO) > 0) {
            membershipPlanName = membership.getPlan().getName();
            membershipPercent = membership.getPlan().getDiscountPercent();
            membershipDiscount = percentageDiscount(afterVoucher, membershipPercent);
        }
        membershipDiscount = clamp(membershipDiscount, 0, afterVoucher);

        return new BookingPricingResult(
                seatTotal,
                foodTotal,
                orderSubtotal,
                null,
                normalizedVoucherCode,
                voucherDiscount,
                membership,
                membershipPlanName,
                membershipPercent,
                afterVoucher,
                membershipDiscount,
                Math.max(0, afterVoucher - membershipDiscount));
    }

    private Optional<UserMembership> activeMembership(User user, LocalDate today) {
        if (!isCustomer(user) || user.getUserId() == null) {
            return Optional.empty();
        }
        return userMembershipRepository.findActiveByUserIdWithPlan(user.getUserId(), STATUS_ACTIVE, today)
                .stream()
                .findFirst();
    }

    private boolean isCustomer(User user) {
        return user != null
                && user.getRole() != null
                && ROLE_CUSTOMER.equalsIgnoreCase(user.getRole().getName());
    }

    private Integer branchId(Showtime showtime) {
        if (showtime == null || showtime.getHall() == null || showtime.getHall().getBranch() == null) {
            return null;
        }
        return showtime.getHall().getBranch().getBranchId();
    }

    private String normalizeVoucherCode(String voucherCode) {
        if (voucherCode == null || voucherCode.isBlank()) {
            return null;
        }
        return voucherCode.trim();
    }

    private long percentageDiscount(long amount, BigDecimal percent) {
        return BigDecimal.valueOf(amount)
                .multiply(percent)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                .longValue();
    }

    private long clamp(long value, long min, long max) {
        return Math.min(max, Math.max(min, value));
    }

    private List<FoodSelection> selectedFood(List<Integer> foodItemIds, List<Integer> foodQuantities) {
        if (foodItemIds == null || foodItemIds.isEmpty()) {
            return List.of();
        }
        if (foodQuantities == null || foodItemIds.size() != foodQuantities.size()) {
            throw new IllegalArgumentException("Thong tin F&B khong hop le");
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
                throw new IllegalArgumentException("San pham F&B hien khong con ban.");
            }
            if (item.getStock() == null || item.getStock() < quantity) {
                throw new IllegalArgumentException("San pham F&B khong con ton kho.");
            }
            selections.add(new FoodSelection(item, quantity));
        }
        return selections;
    }

    private void createPendingFoodOrder(Booking booking, List<FoodSelection> foodSelections) {
        if (foodSelections.isEmpty()) {
            return;
        }

        FoodOrder order = new FoodOrder();
        order.setBooking(booking);
        order.setStatus(FOOD_ORDER_PENDING);
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
