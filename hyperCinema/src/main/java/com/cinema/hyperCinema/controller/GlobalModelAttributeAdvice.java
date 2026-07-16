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

    @ModelAttribute("loggedIn")
    public boolean isLoggedIn(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return userDetails != null && userDetails.getUser() != null;
    }

    @ModelAttribute("isAuthenticated")
    public boolean isAuthenticated(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return userDetails != null && userDetails.getUser() != null;
    }

    @ModelAttribute("dashboardUrl")
    public String resolveDashboardUrl(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getUser() == null) {
            return "/login";
        }
        User user = userDetails.getUser();
        if (user.getRole() == null) {
            return "/";
        }
        return switch (user.getRole().getName().toUpperCase()) {
            case "ADMIN" -> "/admin/dashboard";
            case "MANAGER" -> "/manager/dashboard";
            case "BRANCH_MANAGER", "BRANCHMANAGER" -> "/branch/dashboard";
            case "STAFF" -> "/staff/dashboard";
            case "CUSTOMER" -> "/my/dashboard";
            default -> "/";
        };
    }
}

