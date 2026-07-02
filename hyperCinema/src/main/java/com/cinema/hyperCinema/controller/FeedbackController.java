package com.cinema.hyperCinema.controller;

import com.cinema.hyperCinema.model.Feedback;
import com.cinema.hyperCinema.security.CustomUserDetails;
import com.cinema.hyperCinema.service.feedback.FeedbackService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @GetMapping
    public String listFeedbacks(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Feedback> feedbackPage = feedbackService.getFeedbackList(pageable);
        Map<String, Object> stats = feedbackService.getFeedbackStats();

        model.addAttribute("page", feedbackPage);
        model.addAttribute("stats", stats);

        if (userDetails != null) {
            model.addAttribute("isAuthenticated", true);
            model.addAttribute("currentUserId", userDetails.getUser().getUserId());
        } else {
            model.addAttribute("isAuthenticated", false);
            model.addAttribute("currentUserId", null);
        }

        return "feedback/list";
    }

    @GetMapping("/{id}")
    public String viewFeedbackDetail(
            @PathVariable("id") Integer id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {
        Feedback feedback = feedbackService.getFeedbackDetail(id);
        model.addAttribute("feedback", feedback);

        if (userDetails != null) {
            model.addAttribute("isAuthenticated", true);
            model.addAttribute("currentUserId", userDetails.getUser().getUserId());
        } else {
            model.addAttribute("isAuthenticated", false);
            model.addAttribute("currentUserId", null);
        }

        return "feedback/detail";
    }
}
