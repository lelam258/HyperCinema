package com.cinema.hyperCinema.controller;

import com.cinema.hyperCinema.model.Notification;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.security.CustomUserDetails;
import com.cinema.hyperCinema.service.NotificationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/admin/notifications")
public class AdminNotificationController {

    private final NotificationService notificationService;

    public AdminNotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public String getNotificationsPage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(value = "tab", defaultValue = "receive") String tab,
            Model model) {

        User currentUser = userDetails.getUser();
        List<Notification> notifications = notificationService.getReceivedNotifications(currentUser);
        long unreadCount = notificationService.countUnreadNotifications(currentUser);

        model.addAttribute("tab", tab);
        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("lastUpdated",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));

        return "admin/notifications";
    }
}
