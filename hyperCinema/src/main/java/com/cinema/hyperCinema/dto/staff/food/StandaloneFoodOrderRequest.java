package com.cinema.hyperCinema.dto.staff.food;

import java.util.List;

public record StandaloneFoodOrderRequest(
        List<Integer> foodItemIds,
        List<Integer> foodQuantities,
        String customerPhone,
        String paymentMethod) {
}
