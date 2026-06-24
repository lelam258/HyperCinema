package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    @Query("SELECT n FROM Notification n WHERE n.user.userId = :userId AND (n.scheduledAt IS NULL OR n.scheduledAt <= :now) ORDER BY n.createdAt DESC")
    List<Notification> findReceivedNotifications(@Param("userId") Integer userId, @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.userId = :userId AND n.read = false AND (n.scheduledAt IS NULL OR n.scheduledAt <= :now)")
    long countUnreadNotifications(@Param("userId") Integer userId, @Param("now") LocalDateTime now);
}
