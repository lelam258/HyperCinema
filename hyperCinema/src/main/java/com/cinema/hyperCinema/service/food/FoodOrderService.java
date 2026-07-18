package com.cinema.hyperCinema.service.food;

import com.cinema.hyperCinema.dto.admin.food.request.FoodOrderCreateRequest;
import com.cinema.hyperCinema.dto.admin.food.response.FoodOrderDetailResponse;
import com.cinema.hyperCinema.dto.staff.food.StandaloneFoodOrderRequest;
import com.cinema.hyperCinema.model.User;

import java.util.List;

public interface FoodOrderService {

    /** Tạo đơn đặt đồ ăn gắn với Booking. */
    FoodOrderDetailResponse createOrder(FoodOrderCreateRequest request, User actor);

    FoodOrderDetailResponse createStandaloneOrder(StandaloneFoodOrderRequest request, User actor);

    /** Xác nhận đơn (PENDING → CONFIRMED, trừ Stock). */
    FoodOrderDetailResponse confirmOrder(Integer orderId, User actor);

    /** Hủy đơn PENDING (không thay đổi Stock). */
    FoodOrderDetailResponse cancelPendingOrder(Integer orderId, User actor);

    /** Hủy đơn CONFIRMED (hoàn Stock). */
    FoodOrderDetailResponse cancelConfirmedOrder(Integer orderId, User actor);

    /** Xem chi tiết đơn. */
    FoodOrderDetailResponse findById(Integer orderId, User actor);

    List<FoodOrderDetailResponse> findStandaloneOrders(User actor);
}
