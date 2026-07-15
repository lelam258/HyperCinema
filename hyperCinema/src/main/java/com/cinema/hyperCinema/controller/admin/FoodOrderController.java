package com.cinema.hyperCinema.controller.admin;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cinema.hyperCinema.dto.admin.food.request.FoodOrderCreateRequest;
import com.cinema.hyperCinema.dto.admin.food.response.FoodItemListResponse;
import com.cinema.hyperCinema.dto.admin.food.response.FoodOrderDetailResponse;
import com.cinema.hyperCinema.exception.food.FoodOrderNotFoundException;
import com.cinema.hyperCinema.exception.food.FoodValidationException;
import com.cinema.hyperCinema.model.OrderStatus;
import com.cinema.hyperCinema.repository.FoodOrderRepository;
import com.cinema.hyperCinema.security.CustomUserDetails;
import com.cinema.hyperCinema.service.food.FoodItemService;
import com.cinema.hyperCinema.service.food.FoodOrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/food-orders")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
@RequiredArgsConstructor
public class FoodOrderController {

    private final FoodOrderService foodOrderService;
    private final FoodItemService foodItemService;
    private final FoodOrderRepository foodOrderRepository;

    // -------------------------------------------------------------------------
    // List
    // -------------------------------------------------------------------------

    @GetMapping
    public String list(@AuthenticationPrincipal CustomUserDetails principal,
                       Model model) {
        var orders = foodOrderRepository.findAll();
        model.addAttribute("orders", orders);
        return "admin/food/food-order-list";
    }

    // -------------------------------------------------------------------------
    // Create form
    // -------------------------------------------------------------------------

    @GetMapping("/new")
    public String newForm(@AuthenticationPrincipal CustomUserDetails principal,
                          Model model) {
        model.addAttribute("orderRequest", new FoodOrderCreateRequest());

        // Provide available food items for selection
        List<FoodItemListResponse> availableItems =
                foodItemService.findAll(principal.getUser()).stream()
                        .filter(item -> Boolean.TRUE.equals(item.getIsAvailable()) && item.getStock() > 0)
                        .toList();
        model.addAttribute("availableItems", availableItems);

        return "admin/food/food-order-form";
    }

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    @PostMapping
    public String create(@Valid @ModelAttribute("orderRequest") FoodOrderCreateRequest request,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal CustomUserDetails principal,
                         RedirectAttributes redirectAttributes,
                         Model model) {

        if (bindingResult.hasErrors()) {
            List<FoodItemListResponse> availableItems =
                    foodItemService.findAll(principal.getUser()).stream()
                            .filter(item -> Boolean.TRUE.equals(item.getIsAvailable()) && item.getStock() > 0)
                            .toList();
            model.addAttribute("availableItems", availableItems);
            return "admin/food/food-order-form";
        }

        try {
            FoodOrderDetailResponse order = foodOrderService.createOrder(request, principal.getUser());
            redirectAttributes.addFlashAttribute("successMessage", "food.order.create.success");
            return "redirect:/admin/food-orders/" + order.getOrderId();
        } catch (FoodValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getKey());
            return "redirect:/admin/food-orders/new";
        }
    }

    // -------------------------------------------------------------------------
    // Detail
    // -------------------------------------------------------------------------

    @GetMapping("/{orderId}")
    public String detail(@PathVariable Integer orderId,
                         @AuthenticationPrincipal CustomUserDetails principal,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        try {
            FoodOrderDetailResponse order = foodOrderService.findById(orderId, principal.getUser());
            model.addAttribute("order", order);
            return "admin/food/food-order-detail";
        } catch (FoodOrderNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "food.order.not_found");
            return "redirect:/admin/food-orders";
        }
    }

    // -------------------------------------------------------------------------
    // Confirm
    // -------------------------------------------------------------------------

    @PostMapping("/{orderId}/confirm")
    public String confirm(@PathVariable Integer orderId,
                          @AuthenticationPrincipal CustomUserDetails principal,
                          RedirectAttributes redirectAttributes) {
        try {
            foodOrderService.confirmOrder(orderId, principal.getUser());
            redirectAttributes.addFlashAttribute("successMessage", "food.order.confirmed");
        } catch (FoodOrderNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "food.order.not_found");
        } catch (FoodValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getKey());
        }
        return "redirect:/admin/food-orders/" + orderId;
    }

    // -------------------------------------------------------------------------
    // Cancel
    // -------------------------------------------------------------------------

    @PostMapping("/{orderId}/cancel")
    public String cancel(@PathVariable Integer orderId,
                         @AuthenticationPrincipal CustomUserDetails principal,
                         RedirectAttributes redirectAttributes) {
        try {
            // Determine current status to call correct cancel method
            FoodOrderDetailResponse order = foodOrderService.findById(orderId, principal.getUser());
            if (OrderStatus.CONFIRMED.name().equals(order.getStatus())) {
                foodOrderService.cancelConfirmedOrder(orderId, principal.getUser());
            } else {
                foodOrderService.cancelPendingOrder(orderId, principal.getUser());
            }
            redirectAttributes.addFlashAttribute("successMessage", "food.order.cancelled");
        } catch (FoodOrderNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "food.order.not_found");
        } catch (FoodValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getKey());
        }
        return "redirect:/admin/food-orders/" + orderId;
    }
}
