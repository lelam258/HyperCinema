package com.cinema.hyperCinema.controller.admin;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cinema.hyperCinema.dto.admin.food.request.FoodItemCreateRequest;
import com.cinema.hyperCinema.dto.admin.food.request.FoodItemUpdateRequest;
import com.cinema.hyperCinema.dto.admin.food.response.FoodItemDetailResponse;
import com.cinema.hyperCinema.dto.admin.food.response.FoodItemListResponse;
import com.cinema.hyperCinema.exception.food.FoodItemNotFoundException;
import com.cinema.hyperCinema.exception.food.FoodValidationException;
import com.cinema.hyperCinema.security.CustomUserDetails;
import com.cinema.hyperCinema.service.food.FoodItemService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping({"/admin/food-items", "/admin/food/items"})
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
@RequiredArgsConstructor
public class FoodItemController {

    private final FoodItemService foodItemService;

    // -------------------------------------------------------------------------
    // List + Search
    // -------------------------------------------------------------------------

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String categoryName,
                       @RequestParam(required = false) Boolean isAvailable,
                       @AuthenticationPrincipal CustomUserDetails principal,
                       Model model) {

        List<FoodItemListResponse> items;

        if ((keyword != null && !keyword.isBlank())
                || (categoryName != null && !categoryName.isBlank())
                || isAvailable != null) {
            items = foodItemService.search(keyword, categoryName, isAvailable, principal.getUser());
        } else {
            items = foodItemService.findAll(principal.getUser());
        }

        model.addAttribute("items", items);
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryName", categoryName);
        model.addAttribute("isAvailable", isAvailable);
        model.addAttribute("categories", categories(principal));

        return "admin/food/food-item-list";
    }

    private List<String> categories(CustomUserDetails principal) {
        Set<String> categories = new LinkedHashSet<>(List.of("Combo", "Bắp", "Nước", "Khác"));
        foodItemService.findAll(principal.getUser()).stream()
                .map(FoodItemListResponse::getCategoryName)
                .filter(category -> category != null && !category.isBlank())
                .forEach(categories::add);
        return new ArrayList<>(categories);
    }

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("foodItem", new FoodItemCreateRequest());
        model.addAttribute("mode", "create");
        return "admin/food/food-item-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("foodItem") FoodItemCreateRequest request,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal CustomUserDetails principal,
                         RedirectAttributes redirectAttributes,
                         Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", "create");
            return "admin/food/food-item-form";
        }

        try {
            foodItemService.create(request, principal.getUser());
            redirectAttributes.addFlashAttribute("successMessage", "food.item.create.success");
            return "redirect:/admin/food-items";
        } catch (FoodValidationException ex) {
            bindingResult.reject(ex.getKey());
            model.addAttribute("mode", "create");
            return "admin/food/food-item-form";
        }
    }

    // -------------------------------------------------------------------------
    // Detail
    // -------------------------------------------------------------------------

    @GetMapping("/{itemId}")
    public String detail(@PathVariable Integer itemId,
                         @AuthenticationPrincipal CustomUserDetails principal,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        try {
            FoodItemDetailResponse item = foodItemService.findById(itemId, principal.getUser());
            model.addAttribute("item", item);
            return "admin/food/food-item-detail";
        } catch (FoodItemNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "food.item.not_found");
            return "redirect:/admin/food-items";
        }
    }

    // -------------------------------------------------------------------------
    // Edit
    // -------------------------------------------------------------------------

    @GetMapping("/{itemId}/edit")
    public String editForm(@PathVariable Integer itemId,
                           @AuthenticationPrincipal CustomUserDetails principal,
                           RedirectAttributes redirectAttributes,
                           Model model) {
        try {
            FoodItemDetailResponse item = foodItemService.findById(itemId, principal.getUser());
            FoodItemUpdateRequest updateRequest = new FoodItemUpdateRequest();
            updateRequest.setName(item.getName());
            updateRequest.setCategoryName(item.getCategoryName());
            updateRequest.setDescription(item.getDescription());
            updateRequest.setPrice(item.getPrice());
            updateRequest.setStock(item.getStock());
            updateRequest.setIsAvailable(item.getIsAvailable());
            updateRequest.setImageUrl(item.getImageUrl());

            model.addAttribute("foodItem", updateRequest);
            model.addAttribute("itemId", itemId);
            model.addAttribute("mode", "edit");
            return "admin/food/food-item-form";
        } catch (FoodItemNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "food.item.not_found");
            return "redirect:/admin/food-items";
        }
    }

    @PostMapping("/{itemId}")
    public String update(@PathVariable Integer itemId,
                         @Valid @ModelAttribute("foodItem") FoodItemUpdateRequest request,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal CustomUserDetails principal,
                         RedirectAttributes redirectAttributes,
                         Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("itemId", itemId);
            model.addAttribute("mode", "edit");
            return "admin/food/food-item-form";
        }

        try {
            foodItemService.update(itemId, request, principal.getUser());
            redirectAttributes.addFlashAttribute("successMessage", "food.item.update.success");
            return "redirect:/admin/food-items/" + itemId;
        } catch (FoodItemNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "food.item.not_found");
            return "redirect:/admin/food-items";
        } catch (FoodValidationException ex) {
            bindingResult.reject(ex.getKey());
            model.addAttribute("itemId", itemId);
            model.addAttribute("mode", "edit");
            return "admin/food/food-item-form";
        }
    }

    // -------------------------------------------------------------------------
    // Adjust Stock
    // -------------------------------------------------------------------------

    @PostMapping("/{itemId}/stock")
    public String adjustStock(@PathVariable Integer itemId,
                              @RequestParam Integer newStock,
                              @AuthenticationPrincipal CustomUserDetails principal,
                              RedirectAttributes redirectAttributes) {
        try {
            foodItemService.adjustStock(itemId, newStock, principal.getUser());
            redirectAttributes.addFlashAttribute("successMessage", "food.item.stock.adjusted");
        } catch (FoodItemNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "food.item.not_found");
        } catch (FoodValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getKey());
        }
        return "redirect:/admin/food-items/" + itemId;
    }

    // -------------------------------------------------------------------------
    // Delete (Admin only)
    // -------------------------------------------------------------------------

    @DeleteMapping("/{itemId}")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable Integer itemId,
                         @AuthenticationPrincipal CustomUserDetails principal,
                         RedirectAttributes redirectAttributes) {
        try {
            foodItemService.delete(itemId, principal.getUser());
            redirectAttributes.addFlashAttribute("successMessage", "food.item.delete.success");
        } catch (FoodItemNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "food.item.not_found");
        } catch (FoodValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getKey());
        }
        return "redirect:/admin/food-items";
    }
}
