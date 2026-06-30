package com.cinema.hyperCinema.service.booking.impl;

import com.cinema.hyperCinema.model.*;
import com.cinema.hyperCinema.repository.*;
import com.cinema.hyperCinema.service.booking.BookingService;
import com.cinema.hyperCinema.util.SeatPricing;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private static final String PAYMENT_METHOD_VIETQR = "VietQR";
    private static final String FOOD_ORDER_PENDING = "PENDING";

    private final BookingRepository bookingRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final TicketRepository ticketRepository;
    private final SeatReservationRepository seatReservationRepository;
    private final PaymentRepository paymentRepository;
    private final FoodItemRepository foodItemRepository;
    private final FoodOrderRepository foodOrderRepository;
    private final FoodOrderItemRepository foodOrderItemRepository;

    public BookingServiceImpl(BookingRepository bookingRepository,
                              ShowtimeRepository showtimeRepository,
                              SeatRepository seatRepository,
                              TicketRepository ticketRepository,
                              SeatReservationRepository seatReservationRepository,
                              PaymentRepository paymentRepository,
                              FoodItemRepository foodItemRepository,
                              FoodOrderRepository foodOrderRepository,
                              FoodOrderItemRepository foodOrderItemRepository) {
        this.bookingRepository = bookingRepository;
        this.showtimeRepository = showtimeRepository;
        this.seatRepository = seatRepository;
        this.ticketRepository = ticketRepository;
        this.seatReservationRepository = seatReservationRepository;
        this.paymentRepository = paymentRepository;
        this.foodItemRepository = foodItemRepository;
        this.foodOrderRepository = foodOrderRepository;
        this.foodOrderItemRepository = foodOrderItemRepository;
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
        if (seatIds == null || seatIds.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một ghế.");
        }

        Showtime showtime = findShowtimeWithDetails(showtimeId)
                .orElseThrow(() -> new IllegalStateException("Suất chiếu không tồn tại."));

        if (SHOWTIME_CANCELLED.equals(showtime.getStatus())) {
            throw new IllegalStateException("Suất chiếu không tồn tại.");
        }

        List<Seat> seats = seatRepository.findAllById(seatIds);
        if (seats.size() != seatIds.size()
                || seats.stream().anyMatch(seat -> !seat.getHall().getHallId().equals(showtime.getHall().getHallId()))
                || seats.stream().anyMatch(seat -> MAINTENANCE.equals(seat.getMaintenanceStatus()))
                || hasUnavailableSeats(showtimeId, seatIds)) {
            throw new IllegalArgumentException("Ghế đã được đặt. Vui lòng chọn lại.");
        }

        List<FoodSelection> foodSelections = selectedFood(foodItemIds, foodQuantities);
        long seatTotal = seats.stream()
                .mapToLong(seat -> SeatPricing.priceFor(seat.getType()))
                .sum();
        long foodTotal = foodSelections.stream()
                .mapToLong(selection -> (long) selection.item().getPrice() * selection.quantity())
                .sum();

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setShowtime(showtime);
        booking.setStatus(STATUS_PENDING);
        booking.setTotalPrice(seatTotal + foodTotal);

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
        createPendingFoodOrder(savedBooking, foodSelections);

        Payment payment = new Payment();
        payment.setBooking(savedBooking);
        payment.setAmount(savedBooking.getTotalPrice());
        payment.setMethod(PAYMENT_METHOD_VIETQR);
        payment.setStatus(PAYMENT_PENDING);
        paymentRepository.save(payment);

        return savedBooking;
    }

    private List<FoodSelection> selectedFood(List<Integer> foodItemIds, List<Integer> foodQuantities) {
        if (foodItemIds == null || foodItemIds.isEmpty()) {
            return List.of();
        }
        if (foodQuantities == null || foodItemIds.size() != foodQuantities.size()) {
            throw new IllegalArgumentException("Thông tin F&B không hợp lệ");
        }

        List<FoodSelection> selections = new ArrayList<>();
        for (int i = 0; i < foodItemIds.size(); i++) {
            Integer itemId = foodItemIds.get(i);
            Integer quantity = foodQuantities.get(i);
            if (itemId == null || quantity == null || quantity <= 0) {
                continue;
            }

            FoodItem item = foodItemRepository.findById(itemId)
                    .orElseThrow(() -> new IllegalArgumentException("Sản phẩm F&B không tồn tại."));
            if (!Boolean.TRUE.equals(item.getIsAvailable())) {
                throw new IllegalArgumentException("sản phẩm F&B hiện không còn bán.");
            }
            if (item.getStock() == null || item.getStock() < quantity) {
                throw new IllegalArgumentException("s phẩm F&B không cofn tồn kho.");
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
