package com.cinema.hyperCinema.service.impl;

import com.cinema.hyperCinema.model.Notification;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.model.UserMembership;
import com.cinema.hyperCinema.repository.NotificationRepository;
import com.cinema.hyperCinema.repository.UserMembershipRepository;
import com.cinema.hyperCinema.repository.UserRepository;
import com.cinema.hyperCinema.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final UserMembershipRepository userMembershipRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   UserRepository userRepository,
                                   UserMembershipRepository userMembershipRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.userMembershipRepository = userMembershipRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getReceivedNotifications(User user) {
        return notificationRepository.findReceivedNotifications(user.getUserId(), LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnreadNotifications(User user) {
        return notificationRepository.countUnreadNotifications(user.getUserId(), LocalDateTime.now());
    }

    @Override
    public Notification getNotificationDetails(Integer notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        if (!notification.getUser().getUserId().equals(user.getUserId())) {
            throw new SecurityException("Unauthorized to view this notification");
        }
        if (!notification.getRead()) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
        return notification;
    }

    @Override
    public Notification markAsRead(Integer notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        if (!notification.getUser().getUserId().equals(user.getUserId())) {
            throw new SecurityException("Unauthorized");
        }
        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    @Override
    public Notification markAsUnread(Integer notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        if (!notification.getUser().getUserId().equals(user.getUserId())) {
            throw new SecurityException("Unauthorized");
        }
        notification.setRead(false);
        return notificationRepository.save(notification);
    }

    @Override
    public void sendNotification(String title, String message, String type, List<String> segments, LocalDateTime scheduledAt) {
        Set<User> targetUsers = new HashSet<>();
        LocalDate today = LocalDate.now();
        LocalDateTime cutoffNewUser = LocalDateTime.now().minusDays(7);

        for (String segment : segments) {
            switch (segment.toLowerCase()) {
                case "all":
                case "all users":
                    targetUsers.addAll(userRepository.findAll());
                    break;
                case "admin":
                case "admin users":
                    targetUsers.addAll(userRepository.findAll().stream()
                            .filter(u -> "Admin".equalsIgnoreCase(u.getRole().getName()))
                            .collect(Collectors.toList()));
                    break;
                case "premium":
                case "premium users":
                    List<UserMembership> activeMemberships = userMembershipRepository.findActiveMemberships("Active", today);
                    for (UserMembership um : activeMemberships) {
                        targetUsers.add(um.getUser());
                    }
                    break;
                case "free":
                case "free users":
                    List<User> allUsers = userRepository.findAll();
                    List<User> activePremiumUsers = userMembershipRepository.findActiveMemberships("Active", today)
                            .stream().map(UserMembership::getUser).collect(Collectors.toList());
                    for (User u : allUsers) {
                        if (!activePremiumUsers.contains(u)) {
                            targetUsers.add(u);
                        }
                    }
                    break;
                case "new":
                case "new users":
                    targetUsers.addAll(userRepository.findAll().stream()
                            .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(cutoffNewUser))
                            .collect(Collectors.toList()));
                    break;
            }
        }

        List<Notification> notificationsToSave = new ArrayList<>();
        for (User targetUser : targetUsers) {
            Notification n = new Notification();
            n.setUser(targetUser);
            n.setTitle(title);
            n.setMessage(message);
            n.setType(type);
            n.setRead(false);
            n.setScheduledAt(scheduledAt);
            notificationsToSave.add(n);
        }
        notificationRepository.saveAll(notificationsToSave);
    }
}
