package com.cinema.hyperCinema.service.payment.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.hyperCinema.model.Booking;
import com.cinema.hyperCinema.model.FoodItem;
import com.cinema.hyperCinema.model.FoodOrder;
import com.cinema.hyperCinema.model.FoodOrderItem;
import com.cinema.hyperCinema.model.Payment;
import com.cinema.hyperCinema.repository.BookingRepository;
import com.cinema.hyperCinema.repository.FoodItemRepository;
import com.cinema.hyperCinema.repository.FoodOrderItemRepository;
import com.cinema.hyperCinema.repository.FoodOrderRepository;
import com.cinema.hyperCinema.repository.PaymentRepository;
import com.cinema.hyperCinema.service.payment.BookingPaymentService;

@Service
public class BookingPaymentServiceImpl implements BookingPaymentService {

    private static final String BOOKING_CONFIRMED = "Confirmed";
    private static final String BOOKING_CANCELLED = "Cancelled";
    private static final String TICKET_ACTIVE = "Active";
    private static final String TICKET_CANCELLED = "Cancelled";
    private static final String PAYMENT_COMPLETED = "Completed";
    private static final String PAYMENT_FAILED = "Failed";
    private static final String FOOD_CONFIRMED = "CONFIRMED";
    private static final String FOOD_CANCELLED = "CANCELLED";

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final FoodOrderRepository foodOrderRepository;
    private final FoodOrderItemRepository foodOrderItemRepository;
    private final FoodItemRepository foodItemRepository;

    public BookingPaymentServiceImpl(BookingRepository bookingRepository,
                                     PaymentRepository paymentRepository,
                                     FoodOrderRepository foodOrderRepository,
                                     FoodOrderItemRepository foodOrderItemRepository,
                                     FoodItemRepository foodItemRepository) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.foodOrderRepository = foodOrderRepository;
        this.foodOrderItemRepository = foodOrderItemRepository;
        this.foodItemRepository = foodItemRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> findPaymentByBookingId(Integer bookingId) {
        return paymentRepository.findByBooking_BookingId(bookingId);
    }

    @Override
    @Transactional
    public void confirmPayment(Integer bookingId) {
        Booking booking = findBooking(bookingId);
        booking.setStatus(BOOKING_CONFIRMED);
        if (booking.getTickets() != null) {
            booking.getTickets().forEach(ticket -> ticket.setStatus(TICKET_ACTIVE));
        }
        paymentRepository.findByBooking_BookingId(bookingId)
                .ifPresent(payment -> payment.setStatus(PAYMENT_COMPLETED));
        confirmFoodOrders(bookingId);
    }

    @Override
    @Transactional
    public void failPayment(Integer bookingId) {
        Booking booking = findBooking(bookingId);
        booking.setStatus(BOOKING_CANCELLED);
        if (booking.getTickets() != null) {
            booking.getTickets().forEach(ticket -> ticket.setStatus(TICKET_CANCELLED));
        }
        paymentRepository.findByBooking_BookingId(bookingId)
                .ifPresent(payment -> payment.setStatus(PAYMENT_FAILED));
        cancelFoodOrders(bookingId);
    }

    private Booking findBooking(Integer bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking khong ton tai."));
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
}
