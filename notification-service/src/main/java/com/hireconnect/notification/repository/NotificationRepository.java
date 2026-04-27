package com.hireconnect.notification.repository;

import com.hireconnect.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserId(Long userId);

    List<Notification> findByUserIdAndIsRead(Long userId, boolean isRead);

    long countByUserIdAndIsRead(Long userId, boolean isRead);
    @Transactional
    void deleteByNotificationId(Long notificationId);
}
