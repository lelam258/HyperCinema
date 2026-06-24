package com.cinema.hyperCinema.service.food;

import com.cinema.hyperCinema.dto.admin.food.request.FoodItemCreateRequest;
import com.cinema.hyperCinema.dto.admin.food.request.FoodItemUpdateRequest;
import com.cinema.hyperCinema.dto.admin.food.response.FoodItemDetailResponse;
import com.cinema.hyperCinema.dto.admin.food.response.FoodItemListResponse;
import com.cinema.hyperCinema.model.User;

import java.util.List;

public interface FoodItemService {

    /** Danh sách tất cả mặt hàng, sắp xếp Category_Name → Name (A-Z). */
    List<FoodItemListResponse> findAll(User actor);

    /** Tìm kiếm theo keyword, lọc theo category và availability. */
    List<FoodItemListResponse> search(String keyword, String categoryName,
                                       Boolean isAvailable, User actor);

    /** Xem chi tiết một mặt hàng. */
    FoodItemDetailResponse findById(Integer itemId, User actor);

    /** Tạo mặt hàng mới. Stock phải >= 1. */
    FoodItemDetailResponse create(FoodItemCreateRequest request, User actor);

    /** Cập nhật mặt hàng (bao gồm cả khi is_available = false). */
    FoodItemDetailResponse update(Integer itemId, FoodItemUpdateRequest request, User actor);

    /** Xóa mặt hàng (chỉ Admin, không có lịch sử đặt hàng). */
    void delete(Integer itemId, User actor);

    /** Điều chỉnh Stock trực tiếp (cho phép 0). */
    void adjustStock(Integer itemId, Integer newStock, User actor);
}
