package com.cinema.hyperCinema.controller;

import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.security.CustomUserDetails;
import com.cinema.hyperCinema.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributeAdvice {

    private final NotificationService notificationService;

    @ModelAttribute("unreadNotificationCount")
    public Long getUnreadNotificationCount(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails != null) {
            User user = userDetails.getUser();
            if (user != null) {
                return notificationService.countUnreadNotifications(user);
            }
        }
        return 0L;
    }

    @ModelAttribute("userRole")
    public String getUserRole(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails != null) {
            User user = userDetails.getUser();
            if (user != null && user.getRole() != null) {
                return user.getRole().getName().toUpperCase();
            }
        }
        return "ANONYMOUS";
    }
}
