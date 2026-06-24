package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.FoodItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoodItemRepository extends JpaRepository<FoodItem, Integer> {

    List<FoodItem> findAllByOrderByCategoryNameAscNameAsc();

    List<FoodItem> findByNameContainingIgnoreCase(String keyword);

    List<FoodItem> findByCategoryName(String categoryName);

    List<FoodItem> findByIsAvailable(Boolean isAvailable);

    List<FoodItem> findByIsAvailableTrueAndStockGreaterThanOrderByCategoryNameAscNameAsc(Integer stock);

    List<FoodItem> findByNameContainingIgnoreCaseAndCategoryNameAndIsAvailable(
            String keyword, String categoryName, Boolean isAvailable);
}
