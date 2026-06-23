package com.cinema.hyperCinema.service;

import com.cinema.hyperCinema.model.Notification;
import com.cinema.hyperCinema.model.User;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationService {
    List<Notification> getReceivedNotifications(User user);
    long countUnreadNotifications(User user);
    Notification getNotificationDetails(Integer notificationId, User user);
    Notification markAsRead(Integer notificationId, User user);
    Notification markAsUnread(Integer notificationId, User user);
    void sendNotification(String title, String message, String type, List<String> segments, LocalDateTime scheduledAt);
}
