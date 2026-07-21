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
    public int sendNotification(String title, String message, String type, List<String> segments, LocalDateTime scheduledAt) {
        Map<Integer, User> targetUsers = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        LocalDateTime cutoffNewUser = LocalDateTime.now().minusDays(7);
        List<User> activeUsers = userRepository.findByStatusIgnoreCase("Active");
        List<User> activeCustomers = activeUsers.stream()
                .filter(this::isCustomer)
                .toList();
        Set<Integer> premiumUserIds = userMembershipRepository.findActiveMemberships("Active", today)
                .stream()
                .map(UserMembership::getUser)
                .filter(Objects::nonNull)
                .map(User::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (String segment : segments) {
            if (segment == null) continue;
            switch (segment.trim().toLowerCase(Locale.ROOT)) {
                case "all":
                case "all users":
                    addUsers(targetUsers, activeUsers);
                    break;
                case "admin":
                case "admin users":
                    addUsers(targetUsers, activeUsers.stream()
                            .filter(this::isOperationsUser)
                            .toList());
                    break;
                case "premium":
                case "premium users":
                    addUsers(targetUsers, activeCustomers.stream()
                            .filter(user -> premiumUserIds.contains(user.getUserId()))
                            .toList());
                    break;
                case "free":
                case "free users":
                    addUsers(targetUsers, activeCustomers.stream()
                            .filter(user -> !premiumUserIds.contains(user.getUserId()))
                            .toList());
                    break;
                case "new":
                case "new users":
                    addUsers(targetUsers, activeCustomers.stream()
                            .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(cutoffNewUser))
                            .toList());
                    break;
            }
        }

        List<Notification> notificationsToSave = new ArrayList<>();
        for (User targetUser : targetUsers.values()) {
            notificationsToSave.add(buildNotification(targetUser, title, message, type, scheduledAt));
        }
        notificationRepository.saveAll(notificationsToSave);
        return notificationsToSave.size();
    }

    @Override
    public Notification sendToUser(User user, String title, String message, String type) {
        if (user == null || user.getUserId() == null) {
            throw new IllegalArgumentException("Notification recipient is required");
        }
        return notificationRepository.save(buildNotification(user, title, message, type, null));
    }

    private Notification buildNotification(User user, String title, String message,
                                           String type, LocalDateTime scheduledAt) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setRead(false);
        notification.setScheduledAt(scheduledAt);
        return notification;
    }

    private void addUsers(Map<Integer, User> targets, Collection<User> users) {
        users.stream()
                .filter(Objects::nonNull)
                .filter(user -> user.getUserId() != null)
                .forEach(user -> targets.put(user.getUserId(), user));
    }

    private boolean isCustomer(User user) {
        return user.getRole() != null && "Customer".equalsIgnoreCase(user.getRole().getName());
    }

    private boolean isOperationsUser(User user) {
        if (user.getRole() == null || user.getRole().getName() == null) return false;
        String role = user.getRole().getName().replace("_", "").replace(" ", "");
        return Set.of("admin", "manager", "branchmanager", "staff")
                .contains(role.toLowerCase(Locale.ROOT));
    }
}
