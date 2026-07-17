package com.cinema.hyperCinema.dto.admin.food.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodOrderDetailResponse {

    private Integer orderId;
    private Integer bookingId;
    private Integer branchId;
    private String branchName;
    private Integer staffUserId;
    private String staffName;
    private String customerPhone;
    private String paymentMethod;
    private String paymentStatus;
    private String status;
    private Integer totalAmount;
    private LocalDateTime createdAt;
    private List<FoodOrderItemResponse> items;
}
