package com.cinema.hyperCinema.service.food.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.hyperCinema.dto.admin.food.request.FoodOrderCreateRequest;
import com.cinema.hyperCinema.dto.admin.food.request.FoodOrderItemRequest;
import com.cinema.hyperCinema.dto.admin.food.response.FoodOrderDetailResponse;
import com.cinema.hyperCinema.dto.admin.food.response.FoodOrderItemResponse;
import com.cinema.hyperCinema.exception.food.FoodAccessDeniedException;
import com.cinema.hyperCinema.exception.food.FoodOrderNotFoundException;
import com.cinema.hyperCinema.exception.food.FoodValidationException;
import com.cinema.hyperCinema.model.Booking;
import com.cinema.hyperCinema.model.FoodItem;
import com.cinema.hyperCinema.model.FoodOrder;
import com.cinema.hyperCinema.model.FoodOrderItem;
import com.cinema.hyperCinema.model.OrderStatus;
import com.cinema.hyperCinema.model.Role;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.repository.BookingRepository;
import com.cinema.hyperCinema.repository.FoodItemRepository;
import com.cinema.hyperCinema.repository.FoodOrderItemRepository;
import com.cinema.hyperCinema.repository.FoodOrderRepository;
import com.cinema.hyperCinema.service.food.FoodOrderService;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class FoodOrderServiceImpl implements FoodOrderService {

    private final FoodOrderRepository foodOrderRepository;
    private final FoodOrderItemRepository foodOrderItemRepository;
    private final FoodItemRepository foodItemRepository;
    private final BookingRepository bookingRepository;

    // -------------------------------------------------------------------------
    // Authorization
    // -------------------------------------------------------------------------

    private void assertCanManageOrder(User actor) {
        // Admin, Manager, Staff all allowed
        if (!isAdmin(actor) && !isManager(actor) && !isStaff(actor)) {
            throw new FoodAccessDeniedException();
        }
    }

    // -------------------------------------------------------------------------
    // Create Order
    // -------------------------------------------------------------------------

    @Override
    public FoodOrderDetailResponse createOrder(FoodOrderCreateRequest request, User actor) {
        assertCanManageOrder(actor);

        // Validate booking exists
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new FoodValidationException("food.order.booking.not_found"));

        // Validate and build order items
        List<FoodOrderItem> orderItems = new ArrayList<>();
        int totalAmount = 0;

        for (FoodOrderItemRequest itemRequest : request.getItems()) {
            if (itemRequest.getQuantity() == null || itemRequest.getQuantity() <= 0) {
                throw new FoodValidationException("food.order.quantity.invalid");
            }

            FoodItem foodItem = foodItemRepository.findById(itemRequest.getItemId())
                    .orElseThrow(() -> new FoodValidationException("food.item.not_found"));

            if (!Boolean.TRUE.equals(foodItem.getIsAvailable())) {
                throw new FoodValidationException("food.item.unavailable");
            }

            if (foodItem.getStock() < itemRequest.getQuantity()) {
                throw new FoodValidationException("food.item.insufficient_stock");
            }

            int unitPrice = foodItem.getPrice();
            int subtotal = unitPrice * itemRequest.getQuantity();
            totalAmount += subtotal;

            FoodOrderItem orderItem = new FoodOrderItem();
            orderItem.setItemId(foodItem.getItemId());
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setUnitPrice(unitPrice);

            orderItems.add(orderItem);
        }

        // Create and save order
        FoodOrder order = new FoodOrder();
        order.setBooking(booking);
        order.setStatus(OrderStatus.PENDING.name());
        order.setTotalAmount(totalAmount);

        FoodOrder savedOrder = foodOrderRepository.save(order);

        // Save order items with orderId
        for (FoodOrderItem oi : orderItems) {
            oi.setOrderId(savedOrder.getOrderId());
        }
        foodOrderItemRepository.saveAll(orderItems);

        return buildOrderResponse(savedOrder, orderItems);
    }

    // -------------------------------------------------------------------------
    // Confirm Order
    // -------------------------------------------------------------------------

    @Override
    public FoodOrderDetailResponse confirmOrder(Integer orderId, User actor) {
        assertCanManageOrder(actor);

        FoodOrder order = foodOrderRepository.findById(orderId)
                .orElseThrow(() -> new FoodOrderNotFoundException(orderId));

        if (OrderStatus.CANCELLED.name().equals(order.getStatus())) {
            throw new FoodValidationException("food.order.already_cancelled");
        }

        // Must be PENDING to confirm
        if (!OrderStatus.PENDING.name().equals(order.getStatus())) {
            throw new FoodValidationException("food.order.already_cancelled");
        }

        List<FoodOrderItem> items = foodOrderItemRepository.findByOrderId(orderId);

        // Check stock sufficiency and deduct
        for (FoodOrderItem oi : items) {
            FoodItem foodItem = foodItemRepository.findById(oi.getItemId())
                    .orElseThrow(() -> new FoodValidationException("food.item.not_found"));

            if (foodItem.getStock() < oi.getQuantity()) {
                throw new FoodValidationException("food.order.stock_insufficient");
            }

            foodItem.setStock(foodItem.getStock() - oi.getQuantity());
            foodItemRepository.save(foodItem);
        }

        order.setStatus(OrderStatus.CONFIRMED.name());
        foodOrderRepository.save(order);

        return buildOrderResponse(order, items);
    }

    // -------------------------------------------------------------------------
    // Cancel Pending Order
    // -------------------------------------------------------------------------

    @Override
    public FoodOrderDetailResponse cancelPendingOrder(Integer orderId, User actor) {
        assertCanManageOrder(actor);

        FoodOrder order = foodOrderRepository.findById(orderId)
                .orElseThrow(() -> new FoodOrderNotFoundException(orderId));

        if (OrderStatus.CANCELLED.name().equals(order.getStatus())) {
            throw new FoodValidationException("food.order.already_cancelled");
        }

        if (!OrderStatus.PENDING.name().equals(order.getStatus())) {
            throw new FoodValidationException("food.order.already_cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED.name());
        foodOrderRepository.save(order);

        List<FoodOrderItem> items = foodOrderItemRepository.findByOrderId(orderId);
        return buildOrderResponse(order, items);
    }

    // -------------------------------------------------------------------------
    // Cancel Confirmed Order (restore stock)
    // -------------------------------------------------------------------------

    @Override
    public FoodOrderDetailResponse cancelConfirmedOrder(Integer orderId, User actor) {
        assertCanManageOrder(actor);

        FoodOrder order = foodOrderRepository.findById(orderId)
                .orElseThrow(() -> new FoodOrderNotFoundException(orderId));

        if (OrderStatus.CANCELLED.name().equals(order.getStatus())) {
            throw new FoodValidationException("food.order.already_cancelled");
        }

        if (!OrderStatus.CONFIRMED.name().equals(order.getStatus())) {
            throw new FoodValidationException("food.order.already_cancelled");
        }

        List<FoodOrderItem> items = foodOrderItemRepository.findByOrderId(orderId);

        // Restore stock
        for (FoodOrderItem oi : items) {
            FoodItem foodItem = foodItemRepository.findById(oi.getItemId())
                    .orElseThrow(() -> new FoodValidationException("food.item.not_found"));

            foodItem.setStock(foodItem.getStock() + oi.getQuantity());
            foodItemRepository.save(foodItem);
        }

        order.setStatus(OrderStatus.CANCELLED.name());
        foodOrderRepository.save(order);

        return buildOrderResponse(order, items);
    }

    // -------------------------------------------------------------------------
    // Find by ID
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public FoodOrderDetailResponse findById(Integer orderId, User actor) {
        assertCanManageOrder(actor);

        FoodOrder order = foodOrderRepository.findById(orderId)
                .orElseThrow(() -> new FoodOrderNotFoundException(orderId));

        List<FoodOrderItem> items = foodOrderItemRepository.findByOrderId(orderId);
        return buildOrderResponse(order, items);
    }

    // -------------------------------------------------------------------------
    // Mapping helpers
    // -------------------------------------------------------------------------

    private FoodOrderDetailResponse buildOrderResponse(FoodOrder order, List<FoodOrderItem> items) {
        List<FoodOrderItemResponse> itemResponses = items.stream()
                .map(oi -> {
                    String itemName = "";
                    if (oi.getFoodItem() != null) {
                        itemName = oi.getFoodItem().getName();
                    } else {
                        // Lazy load — fetch name from repo
                        itemName = foodItemRepository.findById(oi.getItemId())
                                .map(FoodItem::getName)
                                .orElse("Unknown");
                    }
                    return FoodOrderItemResponse.builder()
                            .itemId(oi.getItemId())
                            .itemName(itemName)
                            .quantity(oi.getQuantity())
                            .unitPrice(oi.getUnitPrice())
                            .subtotal(oi.getUnitPrice() * oi.getQuantity())
                            .build();
                })
                .collect(Collectors.toList());

        return FoodOrderDetailResponse.builder()
                .orderId(order.getOrderId())
                .bookingId(order.getBooking() != null ? order.getBooking().getBookingId() : null)
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .items(itemResponses)
                .build();
    }

    // -------------------------------------------------------------------------
    // Role helpers
    // -------------------------------------------------------------------------

    private static boolean isAdmin(User user) {
        return isRole(user, "Admin") || isRole(user, "Administrator");
    }

    private static boolean isManager(User user) {
        return isRole(user, "Manager") || isRole(user, "BranchManager")
                || isRole(user, "Branch Manager") || isRole(user, "Branch_Manager");
    }

    private static boolean isStaff(User user) {
        return isRole(user, "Staff");
    }

    private static boolean isRole(User user, String expected) {
        Role role = user.getRole();
        if (role == null) return false;
        return normalizeRoleName(role.getName()).equals(normalizeRoleName(expected));
    }

    private static String normalizeRoleName(String roleName) {
        if (roleName == null) return "";
        String normalized = roleName.trim();
        if (normalized.regionMatches(true, 0, "ROLE_", 0, 5)) {
            normalized = normalized.substring(5);
        }
        return normalized.replaceAll("[\\s_]+", "").toUpperCase();
    }
}
