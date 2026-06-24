package com.cinema.hyperCinema.service.food.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.hyperCinema.dto.admin.food.request.FoodItemCreateRequest;
import com.cinema.hyperCinema.dto.admin.food.request.FoodItemUpdateRequest;
import com.cinema.hyperCinema.dto.admin.food.response.FoodItemDetailResponse;
import com.cinema.hyperCinema.dto.admin.food.response.FoodItemListResponse;
import com.cinema.hyperCinema.exception.food.FoodAccessDeniedException;
import com.cinema.hyperCinema.exception.food.FoodItemNotFoundException;
import com.cinema.hyperCinema.exception.food.FoodValidationException;
import com.cinema.hyperCinema.model.FoodItem;
import com.cinema.hyperCinema.model.Role;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.repository.FoodItemRepository;
import com.cinema.hyperCinema.repository.FoodOrderItemRepository;
import com.cinema.hyperCinema.service.food.FoodItemService;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class FoodItemServiceImpl implements FoodItemService {

    private final FoodItemRepository foodItemRepository;
    private final FoodOrderItemRepository foodOrderItemRepository;

    // -------------------------------------------------------------------------
    // Authorization helpers
    // -------------------------------------------------------------------------

    private void assertCanManageItem(User actor) {
        if (isStaff(actor)) {
            throw new FoodAccessDeniedException();
        }
    }

    private void assertCanDelete(User actor) {
        if (!isAdmin(actor)) {
            throw new FoodAccessDeniedException();
        }
    }

    // -------------------------------------------------------------------------
    // Read operations
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<FoodItemListResponse> findAll(User actor) {
        assertCanManageItem(actor);
        List<FoodItem> items = foodItemRepository.findAllByOrderByCategoryNameAscNameAsc();
        return items.stream().map(this::toListResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FoodItemListResponse> search(String keyword, String categoryName,
                                              Boolean isAvailable, User actor) {
        assertCanManageItem(actor);

        List<FoodItem> items;

        // Combine filters as available
        if (keyword != null && !keyword.isBlank()
                && categoryName != null && !categoryName.isBlank()
                && isAvailable != null) {
            items = foodItemRepository.findByNameContainingIgnoreCaseAndCategoryNameAndIsAvailable(
                    keyword.trim(), categoryName, isAvailable);
        } else if (keyword != null && !keyword.isBlank()) {
            items = foodItemRepository.findByNameContainingIgnoreCase(keyword.trim());
        } else if (categoryName != null && !categoryName.isBlank()) {
            items = foodItemRepository.findByCategoryName(categoryName);
        } else if (isAvailable != null) {
            items = foodItemRepository.findByIsAvailable(isAvailable);
        } else {
            items = foodItemRepository.findAllByOrderByCategoryNameAscNameAsc();
        }

        return items.stream().map(this::toListResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public FoodItemDetailResponse findById(Integer itemId, User actor) {
        assertCanManageItem(actor);
        FoodItem item = foodItemRepository.findById(itemId)
                .orElseThrow(() -> new FoodItemNotFoundException(itemId));
        return toDetailResponse(item);
    }

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    @Override
    public FoodItemDetailResponse create(FoodItemCreateRequest request, User actor) {
        assertCanManageItem(actor);
        validateCreateRequest(request);

        FoodItem item = new FoodItem();
        item.setName(request.getName().trim());
        item.setCategoryName(request.getCategoryName().trim());
        item.setDescription(request.getDescription() != null ? request.getDescription().trim() : "");
        item.setPrice(request.getPrice());
        item.setStock(request.getStock());
        item.setIsAvailable(true);
        item.setImageUrl(request.getImageUrl());

        FoodItem saved = foodItemRepository.save(item);
        return toDetailResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Update
    // -------------------------------------------------------------------------

    @Override
    public FoodItemDetailResponse update(Integer itemId, FoodItemUpdateRequest request, User actor) {
        assertCanManageItem(actor);

        FoodItem item = foodItemRepository.findById(itemId)
                .orElseThrow(() -> new FoodItemNotFoundException(itemId));

        validateUpdateRequest(request);

        item.setName(request.getName().trim());
        item.setCategoryName(request.getCategoryName().trim());
        item.setDescription(request.getDescription() != null ? request.getDescription().trim() : "");
        item.setPrice(request.getPrice());
        item.setStock(request.getStock());
        if (request.getIsAvailable() != null) {
            item.setIsAvailable(request.getIsAvailable());
        }
        item.setImageUrl(request.getImageUrl());

        FoodItem saved = foodItemRepository.save(item);
        return toDetailResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    @Override
    public void delete(Integer itemId, User actor) {
        assertCanDelete(actor);

        FoodItem item = foodItemRepository.findById(itemId)
                .orElseThrow(() -> new FoodItemNotFoundException(itemId));

        if (foodOrderItemRepository.existsByItemId(itemId)) {
            throw new FoodValidationException("food.item.has_order_history");
        }

        foodItemRepository.delete(item);
    }

    // -------------------------------------------------------------------------
    // Stock adjustment
    // -------------------------------------------------------------------------

    @Override
    public void adjustStock(Integer itemId, Integer newStock, User actor) {
        assertCanManageItem(actor);

        FoodItem item = foodItemRepository.findById(itemId)
                .orElseThrow(() -> new FoodItemNotFoundException(itemId));

        if (newStock == null || newStock < 0) {
            throw new FoodValidationException("food.item.stock.invalid");
        }

        item.setStock(newStock);
        foodItemRepository.save(item);
    }

    // -------------------------------------------------------------------------
    // Validation helpers
    // -------------------------------------------------------------------------

    private void validateCreateRequest(FoodItemCreateRequest request) {
        if (request.getName() == null || request.getName().isBlank() || request.getName().trim().length() >= 150) {
            throw new FoodValidationException("food.item.name.invalid");
        }
        if (request.getCategoryName() == null || request.getCategoryName().isBlank()
                || request.getCategoryName().trim().length() > 100) {
            throw new FoodValidationException("food.item.category.invalid");
        }
        if (request.getDescription() != null && request.getDescription().length() > 500) {
            throw new FoodValidationException("food.item.description.invalid");
        }
        if (request.getPrice() == null || request.getPrice() <= 0) {
            throw new FoodValidationException("food.item.price.invalid");
        }
        if (request.getStock() == null || request.getStock() <= 0) {
            throw new FoodValidationException("food.item.stock.invalid_create");
        }
    }

    private void validateUpdateRequest(FoodItemUpdateRequest request) {
        if (request.getName() == null || request.getName().isBlank() || request.getName().trim().length() >= 150) {
            throw new FoodValidationException("food.item.name.invalid");
        }
        if (request.getCategoryName() == null || request.getCategoryName().isBlank()
                || request.getCategoryName().trim().length() > 100) {
            throw new FoodValidationException("food.item.category.invalid");
        }
        if (request.getDescription() != null && request.getDescription().length() > 500) {
            throw new FoodValidationException("food.item.description.invalid");
        }
        if (request.getPrice() == null || request.getPrice() <= 0) {
            throw new FoodValidationException("food.item.price.invalid");
        }
        if (request.getStock() == null || request.getStock() < 0) {
            throw new FoodValidationException("food.item.stock.invalid");
        }
    }

    // -------------------------------------------------------------------------
    // Mapping helpers
    // -------------------------------------------------------------------------

    private FoodItemListResponse toListResponse(FoodItem item) {
        return FoodItemListResponse.builder()
                .itemId(item.getItemId())
                .name(item.getName())
                .categoryName(item.getCategoryName())
                .price(item.getPrice())
                .stock(item.getStock())
                .isAvailable(item.getIsAvailable())
                .outOfStock(item.getStock() == 0)
                .build();
    }

    private FoodItemDetailResponse toDetailResponse(FoodItem item) {
        return FoodItemDetailResponse.builder()
                .itemId(item.getItemId())
                .name(item.getName())
                .categoryName(item.getCategoryName())
                .description(item.getDescription())
                .price(item.getPrice())
                .stock(item.getStock())
                .isAvailable(item.getIsAvailable())
                .imageUrl(item.getImageUrl())
                .build();
    }

    // -------------------------------------------------------------------------
    // Role helpers
    // -------------------------------------------------------------------------

    private static boolean isAdmin(User user) {
        return isRole(user, "Admin") || isRole(user, "Administrator");
    }

    private static boolean isStaff(User user) {
        return isRole(user, "Staff");
    }

    private static boolean isRole(User user, String expected) {
        Role role = user.getRole();
        if (role == null) return false;
        String roleName = normalizeRoleName(role.getName());
        return roleName.equals(normalizeRoleName(expected));
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
