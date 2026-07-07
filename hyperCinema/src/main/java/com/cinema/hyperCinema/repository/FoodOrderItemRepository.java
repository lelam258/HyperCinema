package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.FoodOrderItem;
import com.cinema.hyperCinema.model.FoodOrderItemId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FoodOrderItemRepository extends JpaRepository<FoodOrderItem, FoodOrderItemId> {
}
