package com.cinema.hyperCinema.controller.customer;

import com.cinema.hyperCinema.model.Feedback;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.dto.feedback.FeedbackCreateRequest;
import com.cinema.hyperCinema.dto.feedback.FeedbackUpdateRequest;
import com.cinema.hyperCinema.security.CustomUserDetails;
import com.cinema.hyperCinema.service.feedback.FeedbackService;
import com.cinema.hyperCinema.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/my/feedback")
public class CustomerFeedbackController {

    private final FeedbackService feedbackService;
    private final NotificationService notificationService;

    public CustomerFeedbackController(FeedbackService feedbackService, NotificationService notificationService) {
        this.feedbackService = feedbackService;
        this.notificationService = notificationService;
    }

    @ModelAttribute
    public void addCommonAttributes(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails != null) {
            User user = userDetails.getUser();
            long unreadCount = notificationService.countUnreadNotifications(user);
            model.addAttribute("unreadNotificationCount", unreadCount);
            model.addAttribute("customerName", user.getFullName());
        }
    }

    @GetMapping
    public String feedbackHistory(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User user = userDetails.getUser();
        List<Feedback> feedbacks = feedbackService.getFeedbackHistory(user);

        model.addAttribute("feedbacks", feedbacks);
        model.addAttribute("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));
        return "feedback/history";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        if (!model.containsAttribute("feedbackForm")) {
            model.addAttribute("feedbackForm", new FeedbackCreateRequest());
        }
        model.addAttribute("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));
        return "feedback/new";
    }

    @PostMapping("/new")
    public String createFeedback(
            @Valid @ModelAttribute("feedbackForm") FeedbackCreateRequest request,
            BindingResult bindingResult,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));
            return "feedback/new";
        }

        try {
            feedbackService.createFeedback(request, userDetails.getUser());
            redirectAttributes.addFlashAttribute("successKey", "feedback.create.success");
            return "redirect:/my/feedback";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorKey", "feedback.comment.required");
            return "redirect:/my/feedback/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(
            @PathVariable("id") Integer id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {
        Feedback feedback = feedbackService.getFeedbackDetail(id);

        // Kiểm tra quyền: Chỉ chủ sở hữu mới được sửa
        if (!feedback.getUser().getUserId().equals(userDetails.getUser().getUserId())) {
            return "error/feedback-forbidden";
        }

        if (!model.containsAttribute("feedbackForm")) {
            FeedbackUpdateRequest form = new FeedbackUpdateRequest();
            form.setRating(feedback.getRating());
            form.setComment(feedback.getComment());
            model.addAttribute("feedbackForm", form);
        }

        model.addAttribute("feedbackId", id);
        model.addAttribute("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));
        return "feedback/edit";
    }

    @PostMapping("/{id}/edit")
    public String updateFeedback(
            @PathVariable("id") Integer id,
            @Valid @ModelAttribute("feedbackForm") FeedbackUpdateRequest request,
            BindingResult bindingResult,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes,
            Model model) {
        
        Feedback feedback = feedbackService.getFeedbackDetail(id);
        // Kiểm tra quyền
        if (!feedback.getUser().getUserId().equals(userDetails.getUser().getUserId())) {
            return "error/feedback-forbidden";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("feedbackId", id);
            model.addAttribute("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));
            return "feedback/edit";
        }

        try {
            feedbackService.updateFeedback(id, request, userDetails.getUser());
            redirectAttributes.addFlashAttribute("successKey", "feedback.update.success");
            return "redirect:/my/feedback";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorKey", "feedback.comment.required");
            return "redirect:/my/feedback/" + id + "/edit";
        }
    }
}
