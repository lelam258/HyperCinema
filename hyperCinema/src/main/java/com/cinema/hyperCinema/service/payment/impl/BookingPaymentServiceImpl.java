package com.cinema.hyperCinema.service.payment.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.hyperCinema.model.Booking;
import com.cinema.hyperCinema.model.FoodItem;
import com.cinema.hyperCinema.model.FoodOrder;
import com.cinema.hyperCinema.model.FoodOrderItem;
import com.cinema.hyperCinema.model.LoyaltyPoint;
import com.cinema.hyperCinema.model.Payment;
import com.cinema.hyperCinema.repository.BookingRepository;
import com.cinema.hyperCinema.repository.FoodItemRepository;
import com.cinema.hyperCinema.repository.FoodOrderItemRepository;
import com.cinema.hyperCinema.repository.FoodOrderRepository;
import com.cinema.hyperCinema.repository.LoyaltyPointRepository;
import com.cinema.hyperCinema.repository.PaymentRepository;
import com.cinema.hyperCinema.service.payment.BookingPaymentService;
import com.cinema.hyperCinema.service.payment.PaymentCallbackResult;
import com.cinema.hyperCinema.service.payment.PaymentCallbackResult.Status;

@Service
public class BookingPaymentServiceImpl implements BookingPaymentService {

    private static final String BOOKING_CONFIRMED = "Confirmed";
    private static final String BOOKING_CANCELLED = "Cancelled";
    private static final String TICKET_ACTIVE = "Active";
    private static final String TICKET_CANCELLED = "Cancelled";
    private static final String PAYMENT_COMPLETED = "Completed";
    private static final String PAYMENT_FAILED = "Failed";
    private static final String PAYMENT_PENDING = "Pending";
    private static final String FOOD_CONFIRMED = "CONFIRMED";
    private static final String FOOD_CANCELLED = "CANCELLED";
    private static final long VND_PER_POINT = 10_000L;

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final FoodOrderRepository foodOrderRepository;
    private final FoodOrderItemRepository foodOrderItemRepository;
    private final FoodItemRepository foodItemRepository;
    private final LoyaltyPointRepository loyaltyPointRepository;
    private final long paymentTimeoutMinutes;

    public BookingPaymentServiceImpl(BookingRepository bookingRepository,
                                     PaymentRepository paymentRepository,
                                     FoodOrderRepository foodOrderRepository,
                                     FoodOrderItemRepository foodOrderItemRepository,
                                     FoodItemRepository foodItemRepository,
                                     LoyaltyPointRepository loyaltyPointRepository,
                                     @Value("${booking.payment.timeout-minutes:15}") long paymentTimeoutMinutes) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.foodOrderRepository = foodOrderRepository;
        this.foodOrderItemRepository = foodOrderItemRepository;
        this.foodItemRepository = foodItemRepository;
        this.loyaltyPointRepository = loyaltyPointRepository;
        this.paymentTimeoutMinutes = paymentTimeoutMinutes;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> findPaymentByBookingId(Integer bookingId) {
        return paymentRepository.findByBooking_BookingId(bookingId);
    }

    @Override
    @Transactional
    public void confirmPayment(Integer bookingId) {
        Payment payment = paymentRepository.findByBooking_BookingId(bookingId)
                .orElseThrow(() -> new IllegalStateException("Payment không tồn tại."));
        PaymentCallbackResult result = confirmOnlinePayment(bookingId, safeAmount(payment));
        if (result.accepted()) {
            return;
        }
        throw new IllegalStateException(result.message());
    }

    @Override
    @Transactional
    public PaymentCallbackResult confirmOnlinePayment(Integer bookingId, long callbackAmount) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            return result(Status.ORDER_NOT_FOUND, "Booking không tồn tại.");
        }

        Payment payment = paymentRepository.findByBooking_BookingId(bookingId).orElse(null);
        if (payment == null) {
            return result(Status.PAYMENT_NOT_FOUND, "Payment không tồn tại.");
        }

        long expectedAmount = safeAmount(payment);
        long bookingAmount = discountedTotal(booking);
        if (callbackAmount != expectedAmount && callbackAmount != bookingAmount) {
            return result(Status.INVALID_AMOUNT, "Số tiền thanh toán không khớp.");
        }

        if (PAYMENT_COMPLETED.equals(payment.getStatus())) {
            return result(Status.ALREADY_CONFIRMED, "Payment đã được xác nhận.");
        }

        if (!PAYMENT_PENDING.equals(payment.getStatus())) {
            return result(Status.INVALID_STATE, "Payment không còn ở trạng thái chờ thanh toán.");
        }
        if (isExpired(payment, LocalDateTime.now())) {
            expirePayment(payment);
            return result(Status.EXPIRED, "Payment đã quá hạn thanh toán.");
        }
        payment.setAmount(discountedTotal(booking));
        booking.setStatus(BOOKING_CONFIRMED);
        if (booking.getTickets() != null) {
            booking.getTickets().forEach(ticket -> ticket.setStatus(TICKET_ACTIVE));
        }
        payment.setStatus(PAYMENT_COMPLETED);
        confirmFoodOrders(bookingId);
        awardLoyaltyPoints(booking, payment);
        return result(Status.CONFIRMED, "Payment đã được xác nhận.");
    }

    @Override
    @Transactional
    public void failPayment(Integer bookingId) {
        Payment payment = paymentRepository.findByBooking_BookingId(bookingId).orElse(null);
        failOnlinePayment(bookingId, safeAmount(payment));
    }

    @Override
    @Transactional
    public PaymentCallbackResult failOnlinePayment(Integer bookingId, long callbackAmount) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            return result(Status.ORDER_NOT_FOUND, "Booking không tồn tại.");
        }
        Optional<Payment> payment = paymentRepository.findByBooking_BookingId(bookingId);
        if (payment.isPresent()) {
            long expectedAmount = safeAmount(payment.get());
            long bookingAmount = discountedTotal(booking);
            if (callbackAmount != expectedAmount && callbackAmount != bookingAmount) {
                return result(Status.INVALID_AMOUNT, "Số tiền thanh toán không khớp.");
            }
            if (PAYMENT_COMPLETED.equals(payment.get().getStatus())) {
                return result(Status.ALREADY_CONFIRMED, "Payment đã được xác nhận.");
            }
            expirePayment(payment.get());
            return result(Status.FAILED, "Payment đã được đánh dấu thất bại.");
        }
        cancelBooking(booking);
        cancelFoodOrders(bookingId);
        return result(Status.FAILED, "Booking đã được hủy do thanh toán thất bại.");
    }

    @Override
    @Transactional
    public int expirePendingPayments(LocalDateTime now) {
        LocalDateTime safeNow = now != null ? now : LocalDateTime.now();
        LocalDateTime fallbackCreatedBefore = safeNow.minusMinutes(paymentTimeoutMinutes);
        List<Payment> expiredPayments = paymentRepository.findExpiredPendingPayments(safeNow, fallbackCreatedBefore);
        int expiredCount = 0;
        for (Payment payment : expiredPayments) {
            if (!PAYMENT_PENDING.equals(payment.getStatus()) || !isExpired(payment, safeNow)) {
                continue;
            }
            expirePayment(payment);
            expiredCount++;
        }
        return expiredCount;
    }

    private Booking findBooking(Integer bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking không tồn tại."));
    }

    private void expirePayment(Payment payment) {
        Booking booking = payment.getBooking();
        if (booking != null) {
            cancelBooking(booking);
            cancelFoodOrders(booking.getBookingId());
        }
        payment.setStatus(PAYMENT_FAILED);
    }

    private void cancelBooking(Booking booking) {
        booking.setStatus(BOOKING_CANCELLED);
        if (booking.getTickets() != null) {
            booking.getTickets().forEach(ticket -> ticket.setStatus(TICKET_CANCELLED));
        }
    }

    private boolean isExpired(Payment payment, LocalDateTime now) {
        LocalDateTime safeNow = now != null ? now : LocalDateTime.now();
        if (payment.getExpiresAt() != null) {
            return !payment.getExpiresAt().isAfter(safeNow);
        }
        return payment.getCreatedAt() != null
                && !payment.getCreatedAt().plusMinutes(paymentTimeoutMinutes).isAfter(safeNow);
    }

    private void confirmFoodOrders(Integer bookingId) {
        List<FoodOrder> orders = foodOrderRepository.findByBooking_BookingId(bookingId);
        for (FoodOrder order : orders) {
            if (FOOD_CONFIRMED.equals(order.getStatus())) {
                continue;
            }
            List<FoodOrderItem> items = foodOrderItemRepository.findByOrderId(order.getOrderId());
            for (FoodOrderItem orderItem : items) {
                FoodItem foodItem = foodItemRepository.findById(orderItem.getItemId()).orElse(null);
                if (foodItem != null) {
                    int stock = foodItem.getStock() != null ? foodItem.getStock() : 0;
                    foodItem.setStock(Math.max(0, stock - orderItem.getQuantity()));
                    foodItemRepository.save(foodItem);
                }
            }
            order.setStatus(FOOD_CONFIRMED);
            foodOrderRepository.save(order);
        }
    }

    private void cancelFoodOrders(Integer bookingId) {
        foodOrderRepository.findByBooking_BookingId(bookingId).forEach(order -> {
            if (!FOOD_CONFIRMED.equals(order.getStatus())) {
                order.setStatus(FOOD_CANCELLED);
                foodOrderRepository.save(order);
            }
        });
    }

    private long discountedTotal(Booking booking) {
        return booking != null && booking.getTotalPrice() != null ? Math.max(0L, booking.getTotalPrice()) : 0L;
    }

    private long safeAmount(Payment payment) {
        return payment != null && payment.getAmount() != null ? Math.max(0L, payment.getAmount()) : 0L;
    }

    private PaymentCallbackResult result(Status status, String message) {
        return new PaymentCallbackResult(status, message);
    }

    private void awardLoyaltyPoints(Booking booking, Payment payment) {
        if (booking == null || booking.getBookingId() == null || booking.getUser() == null
                || booking.getUser().getUserId() == null) {
            return;
        }
        long amount = payment != null && payment.getAmount() != null ? payment.getAmount() : discountedTotal(booking);
        int earnedPoints = pointsForAmount(amount);
        if (earnedPoints <= 0) {
            return;
        }
        String type = "BOOKING_" + booking.getBookingId();
        if (loyaltyPointRepository.existsByUser_UserIdAndType(booking.getUser().getUserId(), type)) {
            return;
        }
        LoyaltyPoint loyaltyPoint = new LoyaltyPoint();
        loyaltyPoint.setUser(booking.getUser());
        loyaltyPoint.setPoints(earnedPoints);
        loyaltyPoint.setType(type);
        loyaltyPointRepository.save(loyaltyPoint);
        loyaltyPointRepository.save(loyaltyPoint);
    }

    private int pointsForAmount(long amount) {
        if (amount <= 0) {
            return 0;
        }
        return Math.max(1, (int) (amount / VND_PER_POINT));
    }
}
