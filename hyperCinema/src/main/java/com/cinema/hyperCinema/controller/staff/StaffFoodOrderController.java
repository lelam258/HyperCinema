package com.cinema.hyperCinema.controller.staff;

import com.cinema.hyperCinema.dto.admin.food.response.FoodOrderDetailResponse;
import com.cinema.hyperCinema.dto.staff.food.StandaloneFoodOrderRequest;
import com.cinema.hyperCinema.exception.food.FoodOrderNotFoundException;
import com.cinema.hyperCinema.exception.food.FoodValidationException;
import com.cinema.hyperCinema.security.CustomUserDetails;
import com.cinema.hyperCinema.service.food.FoodOrderService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class StaffFoodOrderController {

    private final FoodOrderService foodOrderService;

    public StaffFoodOrderController(FoodOrderService foodOrderService) {
        this.foodOrderService = foodOrderService;
    }

    @GetMapping("/staff/food-orders")
    public String list(@AuthenticationPrincipal CustomUserDetails userDetails,
                       Model model) {
        List<FoodOrderDetailResponse> orders = foodOrderService.findStandaloneOrders(userDetails.getUser());
        model.addAttribute("orders", orders);
        model.addAttribute("staffName", userDetails.getUser().getFullName());
        model.addAttribute("branchName", userDetails.getUser().getBranch() != null
                ? userDetails.getUser().getBranch().getName()
                : "Chua gan chi nhanh");
        return "staff/food-orders/list";
    }

    @PostMapping("/staff/food-orders")
    public String create(@AuthenticationPrincipal CustomUserDetails userDetails,
                         @RequestParam(name = "foodItemIds", required = false) List<Integer> foodItemIds,
                         @RequestParam(name = "foodQuantities", required = false) List<Integer> foodQuantities,
                         @RequestParam(name = "customerPhone", required = false) String customerPhone,
                         @RequestParam(name = "paymentMethod", required = false) String paymentMethod,
                         RedirectAttributes redirectAttributes) {
        try {
            FoodOrderDetailResponse order = foodOrderService.createStandaloneOrder(
                    new StandaloneFoodOrderRequest(foodItemIds, foodQuantities, customerPhone, paymentMethod),
                    userDetails.getUser());
            redirectAttributes.addFlashAttribute("successMessage", "Da tao don F&B #" + order.getOrderId());
            return "redirect:/staff/food-orders/" + order.getOrderId();
        } catch (FoodValidationException ex) {
            redirectAttributes.addFlashAttribute("bookingError", messageFor(ex.getKey()));
            return "redirect:/staff/booking";
        }
    }

    @GetMapping("/staff/food-orders/{orderId}")
    public String detail(@PathVariable Integer orderId,
                         @AuthenticationPrincipal CustomUserDetails userDetails,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        try {
            FoodOrderDetailResponse order = foodOrderService.findById(orderId, userDetails.getUser());
            if (order.getBookingId() != null) {
                redirectAttributes.addFlashAttribute("bookingError", "Don F&B nay thuoc ve mot booking.");
                return "redirect:/staff/booking";
            }
            model.addAttribute("order", order);
            return "staff/food-orders/detail";
        } catch (FoodOrderNotFoundException ex) {
            redirectAttributes.addFlashAttribute("bookingError", "Khong tim thay don F&B.");
            return "redirect:/staff/booking";
        }
    }

    private String messageFor(String key) {
        return switch (key) {
            case "food.order.branch.required" -> "Nhan vien chua duoc gan chi nhanh.";
            case "food.order.items.required" -> "Vui long chon it nhat mot mon F&B.";
            case "food.order.payment_method.required" -> "Vui long chon phuong thuc thanh toan.";
            case "food.item.unavailable" -> "Mot mon F&B hien khong con ban.";
            case "food.item.insufficient_stock" -> "Mot mon F&B khong con du ton kho.";
            case "food.item.not_found" -> "Khong tim thay mon F&B.";
            default -> "Khong the tao don F&B. Vui long kiem tra lai hoa don.";
        };
    }
}
