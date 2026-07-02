package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.FoodItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoodItemRepository extends JpaRepository<FoodItem, Integer> {
    List<FoodItem> findByIsAvailableTrueAndStockGreaterThanOrderByCategoryNameAscNameAsc(Integer stock);

    List<FoodItem> findByIsAvailableTrueOrderByCategoryNameAscNameAsc();

    List<FoodItem> findAllByOrderByCategoryNameAscNameAsc();

    List<FoodItem> findByNameContainingIgnoreCaseOrderByCategoryNameAscNameAsc(String name);

    List<FoodItem> findByCategoryNameOrderByCategoryNameAscNameAsc(String categoryName);

    List<FoodItem> findByIsAvailableOrderByCategoryNameAscNameAsc(Boolean isAvailable);

    List<FoodItem> findByNameContainingIgnoreCaseAndCategoryNameAndIsAvailableOrderByCategoryNameAscNameAsc(
            String name, String categoryName, Boolean isAvailable);

    default List<FoodItem> findByNameContainingIgnoreCase(String name) {
        return findByNameContainingIgnoreCaseOrderByCategoryNameAscNameAsc(name);
    }

    default List<FoodItem> findByCategoryName(String categoryName) {
        return findByCategoryNameOrderByCategoryNameAscNameAsc(categoryName);
    }

    default List<FoodItem> findByIsAvailable(Boolean isAvailable) {
        return findByIsAvailableOrderByCategoryNameAscNameAsc(isAvailable);
    }

    default List<FoodItem> findByNameContainingIgnoreCaseAndCategoryNameAndIsAvailable(
            String name, String categoryName, Boolean isAvailable) {
        return findByNameContainingIgnoreCaseAndCategoryNameAndIsAvailableOrderByCategoryNameAscNameAsc(
                name, categoryName, isAvailable);
    }
}
