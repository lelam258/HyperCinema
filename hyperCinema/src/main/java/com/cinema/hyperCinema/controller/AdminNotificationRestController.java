package com.cinema.hyperCinema.controller;

import com.cinema.hyperCinema.model.Notification;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.security.CustomUserDetails;
import com.cinema.hyperCinema.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/notifications")
public class AdminNotificationRestController {

    private final NotificationService notificationService;

    public AdminNotificationRestController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getNotificationDetails(
            @PathVariable("id") Integer id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            User currentUser = userDetails.getUser();
            Notification notification = notificationService.getNotificationDetails(id, currentUser);
            long unreadCount = notificationService.countUnreadNotifications(currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("id", notification.getNotificationId());
            response.put("title", notification.getTitle());
            response.put("message", notification.getMessage());
            response.put("type", notification.getType());
            response.put("read", notification.getRead());
            response.put("createdAt", notification.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy, h:mm a")));
            response.put("unreadCount", unreadCount);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/mark-unread/{id}")
    public ResponseEntity<?> markAsUnread(
            @PathVariable("id") Integer id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            User currentUser = userDetails.getUser();
            Notification notification = notificationService.markAsUnread(id, currentUser);
            long unreadCount = notificationService.countUnreadNotifications(currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("id", notification.getNotificationId());
            response.put("read", notification.getRead());
            response.put("unreadCount", unreadCount);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendNotification(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody Map<String, Object> payload) {
        try {
            String title = (String) payload.get("title");
            String message = (String) payload.get("message");
            String type = (String) payload.get("type");
            List<String> segments = (List<String>) payload.get("segments");
            String scheduledAtStr = (String) payload.get("scheduledAt");

            // Input Validation
            if (title == null || title.trim().isEmpty() || title.length() > 100) {
                return ResponseEntity.badRequest().body(Map.of("error", "Title must be between 1 and 100 characters."));
            }
            if (message == null || message.trim().isEmpty() || message.length() > 500) {
                return ResponseEntity.badRequest().body(Map.of("error", "Message must be between 1 and 500 characters."));
            }
            if (type == null || type.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Type is mandatory."));
            }
            if (segments == null || segments.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "At least one recipient segment must be selected."));
            }

            LocalDateTime scheduledAt = null;
            if (scheduledAtStr != null && !scheduledAtStr.trim().isEmpty()) {
                try {
                    scheduledAt = LocalDateTime.parse(scheduledAtStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    if (scheduledAt.isBefore(LocalDateTime.now())) {
                        return ResponseEntity.badRequest().body(Map.of("error", "Scheduled date/time must be in the future."));
                    }
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Invalid date/time format. Use yyyy-MM-ddTHH:mm."));
                }
            }

            notificationService.sendNotification(title, message, type, segments, scheduledAt);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", scheduledAt != null ? "Notification successfully scheduled." : "Notification successfully broadcasted."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
